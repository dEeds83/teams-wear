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
    const allChatIds = await listChatIds(accessToken);
    console.log(`register: user=${userId} chats=${allChatIds.length}`);

    // Subscriptions parallel anlegen (Graph validiert jede synchron gegen unseren
    // Webhook -> sequenziell wuerde die Function timeouten). Auf MAX kappen;
    // restliche Chats abonniert der Renew-Cron nach.
    const MAX_SUBS = 50;
    const chatIds = allChatIds.slice(0, MAX_SUBS);
    const reused: SubInfo[] = [];
    const toCreate: string[] = [];
    for (const chatId of chatIds) {
      const prior = existing?.subscriptions.find((s) => s.chatId === chatId);
      if (prior) reused.push(prior);
      else toCreate.push(chatId);
    }

    const created = await Promise.allSettled(
      toCreate.map(async (chatId) => {
        const { id, expiry } = await createChatSubscription(chatId, accessToken);
        return { id, chatId, expiry } as SubInfo;
      }),
    );
    const subscriptions: SubInfo[] = [...reused];
    for (const r of created) {
      if (r.status === "fulfilled") subscriptions.push(r.value);
      else console.error("Sub fehlgeschlagen", r.reason);
    }
    console.log(`register: subscriptions angelegt=${subscriptions.length}`);

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
