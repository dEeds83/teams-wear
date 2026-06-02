package de.streamonkey.teamswear.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Antwort von /devicecode — enthaelt den vom User einzugebenden Code. */
@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("expires_in") val expiresIn: Long,
    val interval: Long = 5,
    val message: String? = null,
)

/** Antwort von /token (Erfolg ODER Fehler bei pending). */
@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 0,
    @SerialName("token_type") val tokenType: String? = null,
    val scope: String? = null,
    // Fehlerfelder
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)
