# Verifikation

## Phase 0 — Build (automatisierbar)

```bash
cd wear-app
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Relay-Typecheck:

```bash
cd relay && npm install && npm run build
```

## Phase 1 — Lesen/Senden (manuell, ohne Relay)

Voraussetzung: Azure-Schritte 1.1–1.4 + Admin-Consent erledigt, `AZURE_CLIENT_ID`
in `local.properties`.

1. Wear-Emulator (mit Wear-System-Image) anlegen:
   ```bash
   SDK="$HOME/Library/Android/sdk"
   "$SDK/cmdline-tools/latest/bin/sdkmanager" "system-images;android-34;android-wear;arm64-v8a"
   "$SDK/cmdline-tools/latest/bin/avdmanager" create avd -n wear34 \
     -k "system-images;android-34;android-wear;arm64-v8a" -d "wearos_small_round"
   "$SDK/emulator/emulator" -avd wear34 &
   ```
   Emulator braucht Internet (Standalone-Test) — im Emulator WLAN ist per Default da.
2. Installieren: `adb install app/build/outputs/apk/debug/app-debug.apk`.
3. App starten → Login-Screen zeigt **User-Code** + `microsoft.com/devicelogin`.
4. Am PC Code eingeben, mit dem Work/School-Account anmelden.
5. **Erwartet:** Chat-Liste lädt. Chat öffnen → Verlauf. "Antworten" → Chooser
   (Voice / Tastatur / Quick-Replies) → Nachricht erscheint im Teams-Desktop.

## Phase 2 — Push + Inline-Reply (manuell, mit Relay)

Voraussetzung: Firebase (Schritt 3) + Netlify-Relay (Schritt 4) live,
`RELAY_BASE_URL` gesetzt, App neu gebaut/installiert.

1. Nach Login meldet die App FCM-Token + Refresh-Token ans Relay (`/register`);
   Relay legt per-Chat-Subscriptions an. Prüfen: Netlify-Function-Log zeigt
   `subscriptions: N`.
2. Von einem **anderen** Account eine Teams-Nachricht in einen abonnierten Chat
   senden.
3. **Erwartet:** Innerhalb von Sekunden Wear-Notification (Absender + Vorschau).
4. Notification → **Antworten** → Voice/Tastatur/Quick-Reply → Antwort erscheint
   in Teams. Notification verschwindet.
5. **Renewal:** >60 Min laufen lassen; Push muss weiter funktionieren
   (Scheduled Function `renew` erneuert Subscriptions). Netlify-Log prüfen.

## Bekannte manuelle Grenzen

End-to-End lässt sich nicht headless automatisieren: echter Work/School-Account,
Admin-Consent und ein zweiter Teams-Nutzer zum Senden sind nötig.
