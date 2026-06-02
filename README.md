# Teams Wear

Microsoft Teams-Client fuer Wear OS. Zeigt Chat-Liste und Nachrichtenverlauf,
erlaubt Antworten per Voice, Tastatur und Quick-Replies — ohne begleitende
Telefon-App (standalone).

## Funktionsumfang (Phase 1)

- Azure AD Device-Code-Flow: Login direkt auf der Uhr, kein Telefon noetig
- Chat-Liste mit Vorschau der letzten Nachricht (bis 30 Chats)
- Offline-Cache: Chats werden nach dem ersten Laden sofort angezeigt
- Nachrichtenverlauf (bis 30 Nachrichten, neueste unten)
- Antworten: Voice-to-Text, System-Tastatur, vordefinierte Quick-Replies
- Automatischer Token-Refresh im Hintergrund

## Phase 2 (implementiert, benoetigt Firebase + Netlify zur Laufzeit)

- Firebase Cloud Messaging (FCM) fuer Echtzeit-Push (`TeamsMessagingService`)
- Inline-Reply direkt aus der Notification (`ReplyReceiver` + `NotificationPublisher`)
- Netlify-Relay (`relay/`): haelt Graph-Subscriptions, pusht via FCM
- FCM-Token-Registrierung beim Relay (`RelayRepository`, `RELAY_BASE_URL`)

Einrichtung von Azure-Consent, Firebase und Netlify: siehe
[docs/SETUP.md](docs/SETUP.md) und [docs/VERIFY.md](docs/VERIFY.md).

## Konfiguration

Konfigurationswerte werden aus `wear-app/local.properties` gelesen
(nicht eingecheckt) und koennen alternativ als Umgebungsvariablen gesetzt werden.

| Schluessel        | Beschreibung                                                   | Pflicht |
|-------------------|----------------------------------------------------------------|---------|
| `AZURE_CLIENT_ID` | Client-ID der Azure AD App-Registrierung (Public Client)      | ja      |
| `AZURE_TENANT`    | Tenant-ID oder `common` fuer Multi-Tenant                     | nein    |
| `RELAY_BASE_URL`  | Basis-URL des Push-Relay-Servers (Phase 2, leer = deaktiviert)| nein    |

Beispiel `local.properties`:

```
AZURE_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
AZURE_TENANT=common
RELAY_BASE_URL=
```

### Azure AD App-Registrierung

Die App nutzt den **Device-Code-Flow** (Public Client). In der Azure-Registrierung
muss daher "Allow public client flows" aktiviert sein. Benoetigt werden
folgende delegierte Berechtigungen:

- `User.Read`
- `Chat.ReadWrite`
- `ChatMessage.Send`
- `offline_access` (Refresh-Token)

## Build

```bash
cd wear-app
./gradlew assembleDebug
```

Mindest-SDK: API 30 (Wear OS 3). Target-SDK: 34.

## Projektstruktur

```
wear-app/
  app/src/main/java/de/streamonkey/teamswear/
    auth/          # Azure AD OAuth (Device Code Flow, Token-Speicher)
    data/          # ChatRepository, ChatCache, UI-Modelle
    di/            # Hilt-Module (Netzwerk)
    graph/         # Microsoft Graph API (Retrofit), Auth-Interceptor
    notifications/ # FCM-Empfang, Notification-Anzeige, Inline-Reply-Receiver (Phase 2)
    ui/
      chats/       # Chat-Liste
      login/       # Login-Screen (Device Code)
      messages/    # Nachrichtenverlauf + Antworten
      util/        # RemoteInput-Helfer, Zeitformatierung

relay/                       # Netlify-Relay (TypeScript, serverless)
  lib/                       # config, crypto, store (Blobs), graph, fcm
  netlify/functions/         # register, notifications (Webhook), renew (Cron)
docs/                        # SETUP.md, VERIFY.md
```

## Technologie-Stack

| Komponente            | Bibliothek                                   |
|-----------------------|----------------------------------------------|
| UI                    | Jetpack Compose for Wear OS                  |
| Navigation            | `androidx.wear.compose:compose-navigation`   |
| DI                    | Hilt                                         |
| Netzwerk              | Retrofit 2 + OkHttp 4 + kotlinx.serialization|
| Token-Speicher        | EncryptedSharedPreferences (AES-256-GCM)     |
| Chat-Cache            | DataStore Preferences                        |
| Hintergrundaufgaben   | WorkManager + Hilt-Work (Abhaengigkeit vorhanden, kein Worker-Klasse implementiert) |
| Push (Phase 2)        | Firebase Cloud Messaging                     |
