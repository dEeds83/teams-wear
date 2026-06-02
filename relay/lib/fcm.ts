import { cert, getApps, initializeApp, type App } from "firebase-admin/app";
import { getMessaging } from "firebase-admin/messaging";
import { config } from "./config.js";

let app: App | undefined;

function fcmApp(): App {
  if (app) return app;
  if (getApps().length) {
    app = getApps()[0];
    return app;
  }
  const sa = JSON.parse(config.firebaseServiceAccount());
  app = initializeApp({ credential: cert(sa) });
  return app;
}

/**
 * Schickt eine Data-Message an die Watch. Reine Data-Payload (kein notification-
 * Block), damit die App die Wear-Notification selbst mit Inline-Reply baut.
 */
export async function pushToWatch(
  fcmToken: string,
  data: { chatId: string; sender: string; preview: string; messageId: string },
): Promise<void> {
  await getMessaging(fcmApp()).send({
    token: fcmToken,
    data,
    android: { priority: "high" },
  });
}
