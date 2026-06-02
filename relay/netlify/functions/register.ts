import { encrypt } from "../../lib/crypto.js";
import { createChatSubscription, listChatIds, me, refreshAccessToken } from "../../lib/graph.js";
import { getUser, putUser, type SubInfo, type UserRecord } from "../../lib/store.js";

/**
 * POST /register
 * Body: { fcmToken: string, refreshToken: string }
 * Watch meldet ihr FCM-Token + Graph-Refresh-Token an. Relay leitet die userId
 * via /me ab, speichert verschluesselt und legt per-Chat-Subscriptions an.
 */
export default async (req: Request): Promise<Response> => {
  if (req.method !== "POST") return json({ error: "method" }, 405);

  let body: { fcmToken?: string; refreshToken?: string };
  try {
    body = await req.json();
  } catch {
    return json({ error: "bad json" }, 400);
  }
  const { fcmToken, refreshToken } = body;
  if (!fcmToken || !refreshToken) return json({ error: "fcmToken+refreshToken noetig" }, 400);

  try {
    const { accessToken, refreshToken: rotated } = await refreshAccessToken(refreshToken);
    const userId = (await me(accessToken)).id;

    const existing = await getUser(userId);
    const chatIds = await listChatIds(accessToken);
    const subscriptions: SubInfo[] = [];
    for (const chatId of chatIds) {
      // Bestehende Subs wiederverwenden, neue anlegen.
      const prior = existing?.subscriptions.find((s) => s.chatId === chatId);
      if (prior) {
        subscriptions.push(prior);
        continue;
      }
      try {
        const { id, expiry } = await createChatSubscription(chatId, accessToken);
        subscriptions.push({ id, chatId, expiry });
      } catch (e) {
        console.error(`Sub fuer ${chatId} fehlgeschlagen`, e);
      }
    }

    const rec: UserRecord = {
      userId,
      fcmToken,
      refreshTokenEnc: encrypt(rotated ?? refreshToken),
      subscriptions,
      lastChatSync: new Date().toISOString(),
    };
    await putUser(rec);
    return json({ ok: true, userId, subscriptions: subscriptions.length });
  } catch (e) {
    console.error(e);
    return json({ error: String(e) }, 500);
  }
};

function json(obj: unknown, status = 200): Response {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
