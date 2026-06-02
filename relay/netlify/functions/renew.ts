import type { Config } from "@netlify/functions";
import { decrypt, encrypt } from "../../lib/crypto.js";
import {
  createChatSubscription,
  listChatIds,
  refreshAccessToken,
  renewSubscription,
} from "../../lib/graph.js";
import { listUserIds, getUser, putUser, type SubInfo } from "../../lib/store.js";

/**
 * Scheduled (alle 45 Min): erneuert ablaufende Subscriptions und legt Subs fuer
 * neue Chats an. Verhindert Push-Ausfall, da Chat-Subs nach ~60 Min sterben.
 */
export default async (): Promise<Response> => {
  const userIds = await listUserIds();
  for (const userId of userIds) {
    const user = await getUser(userId);
    if (!user) continue;
    try {
      const { accessToken, refreshToken } = await refreshAccessToken(decrypt(user.refreshTokenEnc));
      if (refreshToken) user.refreshTokenEnc = encrypt(refreshToken);

      const soon = Date.now() + 30 * 60_000; // innerhalb 30 Min ablaufend -> erneuern
      for (const sub of user.subscriptions) {
        if (new Date(sub.expiry).getTime() < soon) {
          try {
            sub.expiry = await renewSubscription(sub.id, accessToken);
          } catch (e) {
            console.error(`Renew ${sub.id} fehlgeschlagen, lege neu an`, e);
            const fresh = await createChatSubscription(sub.chatId, accessToken);
            sub.id = fresh.id;
            sub.expiry = fresh.expiry;
          }
        }
      }

      // Neue Chats abonnieren.
      const chatIds = await listChatIds(accessToken);
      const known = new Set(user.subscriptions.map((s) => s.chatId));
      for (const chatId of chatIds) {
        if (known.has(chatId)) continue;
        try {
          const { id, expiry } = await createChatSubscription(chatId, accessToken);
          const sub: SubInfo = { id, chatId, expiry };
          user.subscriptions.push(sub);
        } catch (e) {
          console.error(`Neue Sub ${chatId} fehlgeschlagen`, e);
        }
      }

      user.lastChatSync = new Date().toISOString();
      await putUser(user);
    } catch (e) {
      console.error(`Renewal fuer ${userId} fehlgeschlagen`, e);
    }
  }
  return new Response("ok");
};

export const config: Config = { schedule: "*/45 * * * *" };
