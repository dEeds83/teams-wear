import { decrypt } from "../../lib/crypto.js";
import { deleteSubscription, me, refreshAccessToken } from "../../lib/graph.js";
import { deleteUser, getUser } from "../../lib/store.js";

/**
 * POST /unregister
 * Body: { refreshToken: string }
 * Loescht alle Graph-Subscriptions des Users und den gespeicherten Datensatz.
 * Aufgerufen bei "Push aus" und beim Logout.
 */
export default async (req: Request): Promise<Response> => {
  if (req.method !== "POST") return json({ error: "method" }, 405);

  let body: { refreshToken?: string };
  try {
    body = await req.json();
  } catch {
    return json({ error: "bad json" }, 400);
  }
  if (!body.refreshToken) return json({ error: "refreshToken noetig" }, 400);

  try {
    const { accessToken } = await refreshAccessToken(body.refreshToken);
    const userId = (await me(accessToken)).id;
    const user = await getUser(userId);

    if (user) {
      await Promise.allSettled(
        user.subscriptions.map((s) => deleteSubscription(s.id, accessToken)),
      );
      await deleteUser(userId);
    }
    console.log(`unregister: user=${userId} entfernt`);
    return json({ ok: true });
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
