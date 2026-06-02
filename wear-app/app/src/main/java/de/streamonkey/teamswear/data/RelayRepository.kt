package de.streamonkey.teamswear.data

import com.google.firebase.messaging.FirebaseMessaging
import de.streamonkey.teamswear.BuildConfig
import de.streamonkey.teamswear.auth.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Meldet die Watch beim Netlify-Relay an (FCM-Token + Graph-Refresh-Token),
 * damit das Relay Graph-Subscriptions halten und Push schicken kann.
 * No-op wenn RELAY_BASE_URL leer ist (Push deaktiviert / Phase-1-Betrieb).
 */
@Singleton
class RelayRepository @Inject constructor(
    @Named("auth") private val client: OkHttpClient,
    private val tokenStore: TokenStore,
) {
    private val baseUrl = BuildConfig.RELAY_BASE_URL.trimEnd('/')

    val pushEnabled: Boolean get() = baseUrl.isNotBlank()

    /**
     * Holt das aktuelle FCM-Token und registriert das Geraet beim Relay.
     * No-op (geschluckt) wenn Push deaktiviert oder Firebase nicht konfiguriert.
     */
    suspend fun registerCurrentDevice() {
        if (!pushEnabled) return
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
            ?: return
        registerFcmToken(token)
    }

    /** Registriert ein konkretes FCM-Token + Refresh-Token beim Relay. */
    suspend fun registerFcmToken(fcmToken: String) {
        if (!pushEnabled) return
        val refresh = tokenStore.refreshToken ?: return
        val payload = JSONObject()
            .put("fcmToken", fcmToken)
            .put("refreshToken", refresh)
            .toString()
        val req = Request.Builder()
            .url("$baseUrl/register")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        withContext(Dispatchers.IO) {
            client.newCall(req).execute().use { resp ->
                check(resp.isSuccessful) { "Relay /register: ${resp.code}" }
            }
        }
    }
}
