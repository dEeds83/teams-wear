# Setup — Teams Wear (standalone)

Vollständige Einrichtung von Azure AD, Firebase, Netlify-Relay und der Wear-App.
Reihenfolge einhalten — der Admin-Consent (Schritt 1.3) ist ein harter Blocker.

---

## 1. Azure AD App-Registrierung

### 1.1 App anlegen
1. [Azure Portal](https://portal.azure.com) → **Microsoft Entra ID** → **App registrations** → **New registration**.
2. Name: `Teams Wear`.
3. Supported account types: **Accounts in any organizational directory** (multi-tenant).
4. Redirect URI: leer lassen (Device Code Flow braucht keine).
5. **Register**.

### 1.2 Public Client + Device Code aktivieren
1. **Authentication** → **Advanced settings** → **Allow public client flows** = **Yes**.
2. Speichern.

### 1.3 API-Berechtigungen (Delegated) + Admin-Consent ⚠️
1. **API permissions** → **Add a permission** → **Microsoft Graph** → **Delegated**.
2. Hinzufügen: `User.Read`, `Chat.ReadWrite`, `ChatMessage.Send`, `offline_access`.
3. **Grant admin consent for <Tenant>** klicken.
   - **Fremder Tenant:** Ein Admin der Ziel-Organisation muss zustimmen. Ohne
     Consent schlagen alle Chat-Calls mit `403`/`AADSTS65001` fehl. Das ist der
     zentrale Blocker des Projekts.

### 1.4 Werte notieren
- **Application (client) ID** → `AZURE_CLIENT_ID`
- Tenant: `common` (multi-tenant) oder die konkrete Tenant-ID → `AZURE_TENANT`

---

## 2. Wear-App konfigurieren

`wear-app/local.properties` (nicht eingecheckt):

```properties
sdk.dir=/Users/<du>/Library/Android/sdk
AZURE_CLIENT_ID=<client-id-aus-1.4>
AZURE_TENANT=common
# Erst nach Netlify-Deploy (Schritt 4) setzen — sonst leer lassen (= kein Push):
RELAY_BASE_URL=
```

Bauen:

```bash
cd wear-app
./gradlew :app:assembleDebug
```

> Phase 1 (Chats lesen/senden) läuft komplett ohne Relay/Firebase. Push (Phase 2)
> braucht zusätzlich Schritt 3 + 4 und ein eingebundenes `google-services.json`.

---

## 3. Firebase (FCM) — für Push

1. [Firebase Console](https://console.firebase.google.com) → Projekt anlegen.
2. **Add app** → Android. Package: `de.streamonkey.teamswear`.
3. `google-services.json` herunterladen → nach `wear-app/app/google-services.json`.
4. In `wear-app/app/build.gradle.kts` das Plugin aktivieren:
   `id("com.google.gms.google-services")` (oben einkommentieren).
5. Service-Account-Key für das Relay: **Project Settings** → **Service accounts**
   → **Generate new private key** → JSON. Komplett als Env-Var `FIREBASE_SERVICE_ACCOUNT`
   (Schritt 4).

---

## 4. Netlify-Relay deployen

```bash
cd relay
npm install
npx netlify deploy --prod   # oder Git-Deploy via Netlify-UI
```

Env-Vars in Netlify (**Site settings → Environment variables**):

| Variable | Wert |
|---|---|
| `AZURE_CLIENT_ID` | wie 1.4 |
| `AZURE_TENANT` | `common` oder Tenant-ID |
| `AZURE_CLIENT_SECRET` | leer lassen (public client) |
| `PUBLIC_URL` | `https://<site>.netlify.app` |
| `RELAY_CLIENT_STATE` | langer Zufallsstring (Notification-Validierung) |
| `RELAY_ENC_KEY` | 32-Byte Hex: `openssl rand -hex 32` |
| `FIREBASE_SERVICE_ACCOUNT` | kompletter Service-Account-JSON aus 3.5 |

Danach `RELAY_BASE_URL` in `wear-app/local.properties` auf die Netlify-URL setzen
und die App neu bauen.

---

## 5. Subscription-Modell (Kosten)

Das Relay abonniert **pro Chat** (`/chats/{id}/messages`) — nicht
`/chats/getAllMessages`, da letzteres eine *metered/protected* Graph-API ist.
Neue Chats werden vom Renewal-Cron (alle 45 Min) automatisch nachabonniert.
Chat-Subscriptions leben max ~60 Min; der Cron erneuert sie rechtzeitig.

---

## 6. Verifikation

Siehe [VERIFY.md](VERIFY.md).
