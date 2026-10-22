package com.prslc.zhiflow

import android.app.Application
import android.content.SharedPreferences
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowHardware
import coil3.request.crossfade
import com.prslc.zhiflow.core.native.Natives
import com.prslc.zhiflow.core.network.HeaderProvider
import com.prslc.zhiflow.core.network.HttpClientProvider
import com.prslc.zhiflow.data.session.SessionManager
import com.prslc.zhiflow.di.appModule
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext.startKoin

class Application : Application(), SingletonImageLoader.Factory, KoinComponent {

    private val httpClientProvider: HttpClientProvider by inject()
    private val sessionManager: SessionManager by inject()
    private val sharedPreferences: SharedPreferences by inject()

    override fun onCreate() {
        super.onCreate()

        // First, and on this thread: System.loadLibrary queues on one monitor inside Runtime, so a
        // worker loading its own library first would make the main thread wait.
        Natives.preload()

        // koin
        startKoin {
            androidContext(this@Application)
            modules(appModule)
        }

        // After Koin: the preferences the settled User-Agent is kept in are one of its singles.
        HeaderProvider.init(this, sharedPreferences)

        sessionManager.prefetchGuestSession()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(httpClientProvider.imageClient))
            add(AnimatedImageDecoder.Factory())
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                .maxSizeBytes(64 * 1024 * 1024)
                .build()
        }
        .crossfade(true)
        .allowHardware(true)
        .build()
}
