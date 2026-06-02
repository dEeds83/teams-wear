# Teams Wear

Standalone **Wear-OS-App** für Microsoft Teams — Chats lesen, antworten und
Nachrichten als **Echtzeit-Push** empfangen, **ohne Verbindung zum Smartphone**
(Uhr mit eigenem WLAN/LTE). Es gibt keine offizielle Teams-Wear-App; diese App
spricht direkt die **Microsoft Graph API**. Echtzeit-Push läuft über ein
schlankes serverloses **Relay** (Netlify), das Graph-Subscriptions hält und via
**FCM** an die Uhr pusht — die Uhr selbst bleibt standalone.

Verifiziert end-to-end auf echter Hardware (TicWatch Pro 5, Wear OS, API 33),
multi-tenant inkl. fremdem Tenant mit Admin-Consent.

## Funktionsumfang

- **Login auf der Uhr** via Azure AD **Device-Code-Flow** (kein Telefon nötig),
  multi-tenant (`common`), automatischer Token-Refresh
- **Chat-Liste** (bis 50 Chats) mit Vorschau + relativer Zeit, Offline-Cache;
  1:1-Chats nach dem Gegenüber benannt, lange Titel 2-zeilig
- **Nachrichtenverlauf** — öffnet unten bei der neuesten Nachricht
- **Bilder** im Verlauf inkl. **animierte GIF/WebP** (Coil); Bearer-Token wird
  nur an `graph.microsoft.com` gesendet, nicht an externe CDNs
- **Antworten** — Voice-to-Text, Tastatur, Quick-Replies (RemoteInput)
- **Echtzeit-Push** für eingehende Nachrichten (eigene werden gefiltert),
  **Inline-Reply direkt aus der Notification**
- **Einstellungen** — Push an/aus (meldet sich serverseitig ab/an), Abmelden
- Ereignisgesteuert: kein On-Device-Polling, kein Foreground-Service → akkuschonend

## Architektur

```
[Uhr] --Device-Code-OAuth--> [Azure AD]
[Uhr] --GET chats/messages, POST reply--> [MS Graph]        (direkt, standalone)
[Uhr] --register/unregister {fcm_token, refresh_token}--> [Relay]
[Teams] --> [MS Graph] --webhook--> [Relay] --FCM--> [Uhr]
```

## Module

| Pfad | Inhalt |
|---|---|
| [`wear-app/`](wear-app/) | Wear-OS-App (Kotlin, Jetpack Compose for Wear, Hilt, Retrofit, Coil) |
| [`relay/`](relay/) | Netlify-Relay (TypeScript Functions, Graph-Subscriptions, FCM, Blobs) |
| [`docs/`](docs/) | [Setup](docs/SETUP.md) + [Verifikation](docs/VERIFY.md) |

## Projektstruktur

```
wear-app/app/src/main/java/de/streamonkey/teamswear/
  auth/          # Azure AD Device-Code-Flow, EncryptedSharedPreferences
  data/          # ChatRepository, RelayRepository, ChatCache, SettingsStore
  di/            # Hilt-Module (Netzwerk)
  graph/         # Microsoft Graph (Retrofit), Auth-Interceptor/Authenticator
  notifications/ # FCM-Service, Notification-Anzeige, Inline-Reply-Receiver
  ui/
    chats/       # Chat-Liste
    login/       # Login (Device Code)
    messages/    # Verlauf + Antworten + Bilder
    settings/    # Push-Toggle, Abmelden
    util/        # RemoteInput-Helfer, Zeitformatierung

relay/
  lib/                 # config, crypto, store (Blobs), graph, fcm
  netlify/functions/   # register, unregister, notifications (Webhook), renew (Cron)
```

## Schnellstart

1. Azure-AD-App registrieren (multi-tenant, Public Client, Device-Code) — siehe
   [docs/SETUP.md](docs/SETUP.md) §1. Pro fremdem Tenant einmalig Admin-Consent.
2. `wear-app/local.properties`:
   ```properties
   sdk.dir=/Pfad/zu/Android/sdk
   AZURE_CLIENT_ID=<client-id>
   AZURE_TENANT=common
   RELAY_BASE_URL=            # leer = Phase 1 ohne Push
   ```
3. `cd wear-app && ./gradlew :app:assembleDebug` → Chats lesen/senden läuft.
4. Für Push: Firebase + Netlify-Relay aufsetzen → [docs/SETUP.md](docs/SETUP.md)
   §3–4, dann `RELAY_BASE_URL` setzen + neu bauen.

## Konfiguration (`wear-app/local.properties`, nicht eingecheckt)

| Schlüssel | Beschreibung | Pflicht |
|---|---|---|
| `AZURE_CLIENT_ID` | Client-ID der Azure-AD-Registrierung (Public Client) | ja |
| `AZURE_TENANT` | `common` (multi-tenant) oder Tenant-ID | nein |
| `RELAY_BASE_URL` | Basis-URL des Netlify-Relays (leer = Push aus) | nein |

## Technologie-Stack

| Komponente | Bibliothek |
|---|---|
| UI | Jetpack Compose for Wear OS + Horologist |
| Navigation | `androidx.wear.compose:compose-navigation` |
| DI | Hilt |
| Netzwerk | Retrofit 2 + OkHttp 4 + kotlinx.serialization |
| Bilder | Coil (+ GIF-Decoder) |
| Token-Speicher | EncryptedSharedPreferences (AES-256-GCM) |
| Cache / Settings | DataStore Preferences |
| Push | Firebase Cloud Messaging |
| Relay | Netlify Functions (TypeScript), Netlify Blobs, firebase-admin |

## ⚠️ Voraussetzungen

- **Admin-Consent pro Tenant** für `Chat.ReadWrite` / `ChatMessage.Send`, falls
  der Tenant User-Consent sperrt. Ohne das kein Chat-Zugriff.
- Push benötigt zur Laufzeit `wear-app/app/google-services.json` + aktiviertes
  `com.google.gms.google-services`-Plugin sowie ein deploytes Relay.

## Bekannte Grenzen

- Voice-Calls sind nicht umsetzbar (Graph bietet keine delegierte Call-API; kein
  Wear-OS-Calling-SDK).
- Relay abonniert pro Login die neuesten bis zu 50 Chats; weitere holt der
  Renewal-Cron (alle 45 min) nach. Chat-Subscriptions leben ~60 min und werden
  vom Cron erneuert.
