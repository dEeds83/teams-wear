package de.streamonkey.teamswear.auth

import de.streamonkey.teamswear.BuildConfig
import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/** Ergebnis des Device-Code-Pollings. */
sealed interface LoginResult {
    data object Success : LoginResult
    data class Failed(val reason: String) : LoginResult
}

/**
 * Einzige Quelle fuer Auth-State. Orchestriert Device-Code-Flow, Token-Polling
 * und Refresh gegen Azure AD v2.0. Speichert Tokens via [TokenStore].
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: AzureAuthApi,
    private val tokenStore: TokenStore,
) {
    private val tenant get() = BuildConfig.AZURE_TENANT
    private val clientId get() = BuildConfig.AZURE_CLIENT_ID

    val isLoggedIn: Boolean get() = tokenStore.isLoggedIn

    /** Startet den Flow: liefert User-Code + Verification-URI fuer die Anzeige. */
    suspend fun beginDeviceCode(): DeviceCodeResponse =
        api.deviceCode(tenant, clientId, SCOPE)

    /**
     * Pollt das Token-Endpoint bis der User den Code bestaetigt hat, abgelaufen
     * ist oder abgelehnt wurde. Speichert Tokens bei Erfolg.
     */
    suspend fun pollForToken(dc: DeviceCodeResponse): LoginResult {
        var intervalMs = dc.interval * 1000
        val deadline = nowMs() + dc.expiresIn * 1000
        while (nowMs() < deadline) {
            delay(intervalMs)
            val resp = try {
                api.deviceToken(tenant = tenant, clientId = clientId, deviceCode = dc.deviceCode)
            } catch (e: HttpException) {
                parseError(e) ?: return LoginResult.Failed("HTTP ${e.code()}")
            }
            when (resp.error) {
                null -> {
                    if (resp.accessToken != null) {
                        tokenStore.save(resp.accessToken, resp.refreshToken, resp.expiresIn, nowMs())
                        return LoginResult.Success
                    }
                }
                "authorization_pending" -> { /* weiter warten */ }
                "slow_down" -> intervalMs += 5_000
                "expired_token" -> return LoginResult.Failed("Code abgelaufen")
                "authorization_declined" -> return LoginResult.Failed("Abgelehnt")
                else -> return LoginResult.Failed(resp.errorDescription ?: resp.error!!)
            }
        }
        return LoginResult.Failed("Zeitueberschreitung")
    }

    /**
     * Liefert ein gueltiges Access-Token; erneuert via Refresh-Token wenn noetig.
     * Wirft IllegalStateException wenn nicht eingeloggt / Refresh fehlschlaegt.
     * Synchron nutzbar (auch aus OkHttp-Authenticator via runBlocking).
     */
    suspend fun validAccessToken(): String {
        if (tokenStore.hasValidAccessToken(nowMs())) return tokenStore.accessToken!!
        return refreshNow()
    }

    /** Erzwingt sofortigen Refresh; wirft bei fehlendem Refresh-Token oder Fehlerantwort. */
    suspend fun refreshNow(): String {
        val rt = tokenStore.refreshToken ?: throw IllegalStateException("Nicht eingeloggt")
        val resp = api.refresh(tenant = tenant, clientId = clientId, refreshToken = rt, scope = SCOPE)
        val at = resp.accessToken
            ?: throw IllegalStateException("Refresh fehlgeschlagen: ${resp.error}")
        tokenStore.save(at, resp.refreshToken, resp.expiresIn, nowMs())
        return at
    }

    /** Loescht alle gespeicherten Tokens; Benutzer muss sich erneut anmelden. */
    fun logout() = tokenStore.clear()

    private fun parseError(e: HttpException): TokenResponse? = try {
        e.response()?.errorBody()?.string()?.let { json.decodeFromString<TokenResponse>(it) }
    } catch (_: Exception) { null }

    private fun nowMs() = System.currentTimeMillis()

    companion object {
        // Delegated Graph-Scopes. offline_access -> Refresh-Token.
        const val SCOPE = "offline_access User.Read Chat.ReadWrite ChatMessage.Send"
        private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    }
}
