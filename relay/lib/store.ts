import { getStore } from "@netlify/blobs";

/** Eine abonnierte Graph-Subscription fuer einen Chat. */
export interface SubInfo {
  id: string;
  chatId: string;
  expiry: string; // ISO-8601
}

/** Persistierter Datensatz pro angemeldetem User. */
export interface UserRecord {
  userId: string;
  fcmToken: string;
  refreshTokenEnc: string;
  subscriptions: SubInfo[];
  lastChatSync?: string;
}

const USERS = "users"; // key: userId -> UserRecord
const SUBS = "subs"; // key: subscriptionId -> userId (Reverse-Index)

function users() {
  return getStore({ name: USERS, consistency: "strong" });
}
function subs() {
  return getStore({ name: SUBS, consistency: "strong" });
}

export async function getUser(userId: string): Promise<UserRecord | null> {
  return (await users().get(userId, { type: "json" })) as UserRecord | null;
}

export async function putUser(rec: UserRecord): Promise<void> {
  await users().setJSON(rec.userId, rec);
  // Reverse-Index aktuell halten.
  await Promise.all(rec.subscriptions.map((s) => subs().set(s.id, rec.userId)));
}

export async function userIdForSub(subscriptionId: string): Promise<string | null> {
  return await subs().get(subscriptionId);
}

export async function listUserIds(): Promise<string[]> {
  const { blobs } = await users().list();
  return blobs.map((b) => b.key);
}
