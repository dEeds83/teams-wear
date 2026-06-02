package de.streamonkey.teamswear.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.streamonkey.teamswear.graph.AuthInterceptor
import de.streamonkey.teamswear.graph.GraphApi
import de.streamonkey.teamswear.graph.TokenAuthenticator
import de.streamonkey.teamswear.auth.AzureAuthApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ignoreUnknownKeys: Graph-Antworten enthalten oft odata-Felder und Beta-Properties,
    // die wir nicht modellieren. explicitNulls = false spart Bandbreite beim Senden.
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val contentType = "application/json".toMediaType()

    @Provides
    @Singleton
    fun json(): Json = json

    /**
     * Client OHNE Auth-Interceptor — fuer Azure-Login-Endpoints und das Relay
     * ([RelayRepository] injectet diesen Client ebenfalls via @Named("auth")).
     */
    @Provides
    @Singleton
    @Named("auth")
    fun authClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun azureAuthApi(@Named("auth") client: OkHttpClient): AzureAuthApi =
        Retrofit.Builder()
            .baseUrl("https://login.microsoftonline.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(AzureAuthApi::class.java)

    /** Client MIT Auth-Interceptor + Authenticator — fuer Graph. */
    @Provides
    @Singleton
    @Named("graph")
    fun graphClient(
        interceptor: AuthInterceptor,
        authenticator: TokenAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .authenticator(authenticator)
        .build()

    @Provides
    @Singleton
    fun graphApi(@Named("graph") client: OkHttpClient): GraphApi =
        Retrofit.Builder()
            .baseUrl("https://graph.microsoft.com/v1.0/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GraphApi::class.java)
}
