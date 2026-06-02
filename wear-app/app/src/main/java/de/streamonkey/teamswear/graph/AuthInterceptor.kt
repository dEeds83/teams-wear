package de.streamonkey.teamswear.graph

import de.streamonkey.teamswear.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Haengt das aktuelle Bearer-Token an jede Graph-Anfrage.
 * Schlaegt das Token-Abrufen fehl (kein Login, kein Netz), wird die Anfrage
 * ohne Authorization-Header durchgelassen — [TokenAuthenticator] faengt dann
 * den 401 ab und versucht einen Refresh.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val auth: AuthRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { runCatching { auth.validAccessToken() }.getOrNull() }
        val req = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else chain.request()
        return chain.proceed(req)
    }
}

/** Bei 401 einmal Token erneuern und Anfrage wiederholen. */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val auth: AuthRepository,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) return null
        if (responseCount(response) >= 2) return null // Schleife vermeiden
        val newToken = runBlocking { runCatching { auth.refreshNow() }.getOrNull() } ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) { count++; r = r.priorResponse }
        return count
    }
}
