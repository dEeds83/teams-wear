import { config } from "../../lib/config.js";
import { decrypt, encrypt } from "../../lib/crypto.js";
import { getMessageByResource, refreshAccessToken } from "../../lib/graph.js";
import { pushToWatch } from "../../lib/fcm.js";
import { getUser, putUser, userIdForSub } from "../../lib/store.js";

interface Notification {
  subscriptionId: string;
  clientState?: string;
  resource: string;
  resourceData?: { id?: string };
}

/**
 * POST /notifications — Graph-Webhook.
 * 1) Validation-Handshake: ?validationToken=... -> Token als text/plain echoen.
 * 2) Aenderungs-Notification: clientState pruefen, Nachricht holen, FCM pushen.
 */
export default async (req: Request): Promise<Response> => {
  const url = new URL(req.url);
  const validationToken = url.searchParams.get("validationToken");
  if (validationToken) {
    return new Response(validationToken, {
      status: 200,
      headers: { "Content-Type": "text/plain" },
    });
  }

  if (req.method !== "POST") return new Response("method", { status: 405 });

  let payload: { value?: Notification[] };
  try {
    payload = await req.json();
  } catch {
    return new Response("bad json", { status: 400 });
  }

  // Graph erwartet schnelle 202; Verarbeitung best-effort, Fehler nur loggen.
  for (const n of payload.value ?? []) {
    try {
      if (n.clientState !== config.clientState()) {
        console.warn("clientState mismatch, ignoriere");
        continue;
      }
      const userId = await userIdForSub(n.subscriptionId);
      if (!userId) continue;
      const user = await getUser(userId);
      if (!user) continue;

      const { accessToken, refreshToken } = await refreshAccessToken(decrypt(user.refreshTokenEnc));
      if (refreshToken) {
        user.refreshTokenEnc = encrypt(refreshToken);
        await putUser(user);
      }

      const msg = await getMessageByResource(n.resource, accessToken);
      const preview = stripHtml(msg.body?.content ?? "");
      const chatId = msg.chatId ?? chatIdFromResource(n.resource);
      await pushToWatch(user.fcmToken, {
        chatId,
        sender: msg.from?.user?.displayName ?? "Teams",
        preview: preview.slice(0, 200),
        messageId: n.resourceData?.id ?? "",
      });
    } catch (e) {
      console.error("Notification-Verarbeitung fehlgeschlagen", e);
    }
  }

  return new Response("", { status: 202 });
};

function chatIdFromResource(resource: string): string {
  // resource z.B. "chats('19:...@thread.v2')/messages('...')"
  const m = resource.match(/chats\('([^']+)'\)/) ?? resource.match(/chats\/([^/]+)\/messages/);
  return m?.[1] ?? "";
}

function stripHtml(s: string): string {
  return s.replace(/<[^>]*>/g, "").replace(/&nbsp;/g, " ").trim();
}
