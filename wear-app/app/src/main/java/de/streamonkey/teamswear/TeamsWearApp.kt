package de.streamonkey.teamswear

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import de.streamonkey.teamswear.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

@HiltAndroidApp
class TeamsWearApp : Application(), ImageLoaderFactory {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ImageEntryPoint {
        fun authRepository(): AuthRepository
    }

    /**
     * Coil-ImageLoader fuer Chat-Bilder. Graph-hostedContents brauchen ein
     * Bearer-Token — das wird NUR an graph.microsoft.com angehaengt, damit es
     * nicht an externe CDNs (Giphy etc.) leakt. Animierte GIF/WebP via Decoder.
     */
    override fun newImageLoader(): ImageLoader {
        val auth = EntryPointAccessors
            .fromApplication(this, ImageEntryPoint::class.java)
            .authRepository()

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val out = if (req.url.host == "graph.microsoft.com") {
                    val token = runBlocking { runCatching { auth.validAccessToken() }.getOrNull() }
                    if (token != null) {
                        req.newBuilder().header("Authorization", "Bearer $token").build()
                    } else req
                } else req
                chain.proceed(out)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }
}
