package de.streamonkey.teamswear.auth

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Azure AD v2.0 OAuth — Device Code Flow + Refresh.
 * Base-URL: https://login.microsoftonline.com/
 */
interface AzureAuthApi {

    @FormUrlEncoded
    @POST("{tenant}/oauth2/v2.0/devicecode")
    suspend fun deviceCode(
        @Path("tenant") tenant: String,
        @Field("client_id") clientId: String,
        @Field("scope") scope: String,
    ): DeviceCodeResponse

    @FormUrlEncoded
    @POST("{tenant}/oauth2/v2.0/token")
    suspend fun deviceToken(
        @Path("tenant") tenant: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code",
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
    ): TokenResponse

    @FormUrlEncoded
    @POST("{tenant}/oauth2/v2.0/token")
    suspend fun refresh(
        @Path("tenant") tenant: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
        @Field("scope") scope: String,
    ): TokenResponse
}
