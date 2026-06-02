/** Zentrale Env-Konfiguration des Relays. Wirft frueh bei fehlenden Pflichtwerten. */

function required(name: string): string {
  const v = process.env[name];
  if (!v) throw new Error(`Env ${name} fehlt`);
  return v;
}

export const config = {
  // Azure AD (gleiche App-Registrierung wie die Watch).
  azureTenant: process.env.AZURE_TENANT ?? "common",
  azureClientId: () => required("AZURE_CLIENT_ID"),
  // Public client (Device Code) hat kein Secret. Confidential optional.
  azureClientSecret: process.env.AZURE_CLIENT_SECRET ?? "",
  graphScope: "offline_access User.Read Chat.ReadWrite ChatMessage.Send",

  // Oeffentliche Basis-URL dieser Netlify-Site (fuer Graph notificationUrl).
  publicUrl: () => required("PUBLIC_URL"),

  // Geheimer Wert zum Validieren eingehender Graph-Notifications.
  clientState: () => required("RELAY_CLIENT_STATE"),

  // 32-Byte Hex-Key fuer AES-256-GCM (Refresh-Token-Verschluesselung).
  encKeyHex: () => required("RELAY_ENC_KEY"),

  // Firebase Service-Account JSON (komplett als eine Env-Var).
  firebaseServiceAccount: () => required("FIREBASE_SERVICE_ACCOUNT"),
};

export const GRAPH = "https://graph.microsoft.com/v1.0";
export function tokenEndpoint() {
  return `https://login.microsoftonline.com/${config.azureTenant}/oauth2/v2.0/token`;
}
