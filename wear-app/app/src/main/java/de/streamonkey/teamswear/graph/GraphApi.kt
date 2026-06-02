package de.streamonkey.teamswear.graph

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Microsoft Graph v1.0. Base-URL: https://graph.microsoft.com/v1.0/
 * Auth via AuthInterceptor (Bearer).
 */
interface GraphApi {

    @GET("me")
    suspend fun me(): GraphUser

    /** Chat-Liste, neueste zuerst, inkl. Members + Last-Message-Preview. */
    @GET("me/chats")
    suspend fun chats(
        @Query("\$expand") expand: String = "members",
        @Query("\$top") top: Int = 50,
        @Query("\$orderby") orderBy: String = "lastMessagePreview/createdDateTime desc",
    ): GraphList<Chat>

    /** Nachrichtenverlauf eines Chats (bis [top] Eintraege, Graph liefert neueste zuerst). */
    @GET("chats/{chatId}/messages")
    suspend fun messages(
        @Path("chatId") chatId: String,
        @Query("\$top") top: Int = 30,
    ): GraphList<ChatMessage>

    /** Antwort senden. */
    @POST("chats/{chatId}/messages")
    suspend fun sendMessage(
        @Path("chatId") chatId: String,
        @Body body: SendMessageBody,
    ): ChatMessage
}
