import { config, GRAPH, tokenEndpoint } from "./config.js";

/** Erneuert ein Access-Token aus einem Refresh-Token. Liefert {accessToken, refreshToken?}. */
export async function refreshAccessToken(
  refreshToken: string,
): Promise<{ accessToken: string; refreshToken?: string }> {
  const body = new URLSearchParams({
    grant_type: "refresh_token",
    client_id: config.azureClientId(),
    refresh_token: refreshToken,
    scope: config.graphScope,
  });
  if (config.azureClientSecret) body.set("client_secret", config.azureClientSecret);

  const res = await fetch(tokenEndpoint(), {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body,
  });
  if (!res.ok) throw new Error(`Token-Refresh fehlgeschlagen: ${res.status} ${await res.text()}`);
  const json = (await res.json()) as { access_token: string; refresh_token?: string };
  return { accessToken: json.access_token, refreshToken: json.refresh_token };
}

async function graphGet<T>(path: string, accessToken: string): Promise<T> {
  const res = await fetch(`${GRAPH}${path}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!res.ok) throw new Error(`Graph GET ${path}: ${res.status} ${await res.text()}`);
  return (await res.json()) as T;
}

export async function me(accessToken: string): Promise<{ id: string }> {
  return graphGet<{ id: string }>("/me", accessToken);
}

export async function listChatIds(accessToken: string): Promise<string[]> {
  const data = await graphGet<{ value: { id: string }[] }>(
    "/me/chats?$top=50&$select=id",
    accessToken,
  );
  return data.value.map((c) => c.id);
}

/** Holt eine Nachricht ueber den resource-Pfad aus der Notification. */
export async function getMessageByResource(
  resource: string,
  accessToken: string,
): Promise<{ from?: { user?: { displayName?: string } }; body?: { content?: string }; chatId?: string }> {
  return graphGet(`/${resource}`, accessToken);
}

/** Maximale Lebensdauer einer Chat-Message-Subscription: ~60 Min. */
function subExpiry(minutes = 55): string {
  return new Date(Date.now() + minutes * 60_000).toISOString();
}

export async function createChatSubscription(
  chatId: string,
  accessToken: string,
): Promise<{ id: string; expiry: string }> {
  const expiry = subExpiry();
  const res = await fetch(`${GRAPH}/subscriptions`, {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      changeType: "created",
      notificationUrl: `${config.publicUrl()}/notifications`,
      resource: `/chats/${chatId}/messages`,
      expirationDateTime: expiry,
      clientState: config.clientState(),
    }),
  });
  if (!res.ok) throw new Error(`Subscription anlegen: ${res.status} ${await res.text()}`);
  const json = (await res.json()) as { id: string; expirationDateTime: string };
  return { id: json.id, expiry: json.expirationDateTime };
}

export async function renewSubscription(
  subscriptionId: string,
  accessToken: string,
): Promise<string> {
  const expiry = subExpiry();
  const res = await fetch(`${GRAPH}/subscriptions/${subscriptionId}`, {
    method: "PATCH",
    headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
    body: JSON.stringify({ expirationDateTime: expiry }),
  });
  if (!res.ok) throw new Error(`Subscription renew: ${res.status} ${await res.text()}`);
  return expiry;
}
