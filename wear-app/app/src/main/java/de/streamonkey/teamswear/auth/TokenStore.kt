package de.streamonkey.teamswear.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verschluesselter Speicher fuer OAuth-Tokens (EncryptedSharedPreferences,
 * AES256). Haelt Access-Token, Refresh-Token und Ablaufzeitpunkt.
 */
@Singleton
class TokenStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "teams_wear_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    val accessToken: String? get() = prefs.getString(KEY_ACCESS, null)
    val refreshToken: String? get() = prefs.getString(KEY_REFRESH, null)
    val expiresAtEpochMs: Long get() = prefs.getLong(KEY_EXPIRES, 0L)

    val isLoggedIn: Boolean get() = refreshToken != null

    /** Token gilt als gueltig, wenn es noch >60s laeuft. */
    fun hasValidAccessToken(nowMs: Long): Boolean =
        accessToken != null && nowMs < expiresAtEpochMs - 60_000

    /**
     * Speichert ein neues Access-Token und aktualisiert den Ablaufzeitpunkt.
     * Refresh-Token wird nur ueberschrieben, wenn Azure ein neues geliefert hat
     * (Rotation ist optional — Azure rotiert nicht bei jedem Grant).
     */
    fun save(accessToken: String, refreshToken: String?, expiresInSec: Long, nowMs: Long) {
        prefs.edit().apply {
            putString(KEY_ACCESS, accessToken)
            // Azure rotiert Refresh-Tokens; nur ueberschreiben wenn neues kam.
            if (refreshToken != null) putString(KEY_REFRESH, refreshToken)
            putLong(KEY_EXPIRES, nowMs + expiresInSec * 1000)
        }.apply()
    }

    /** Loescht alle gespeicherten Schluessel (Logout). */
    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EXPIRES = "expires_at"
    }
}
