package com.prslc.zhiflow

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowHardware
import coil3.request.crossfade
import com.prslc.zhiflow.core.network.Client
import com.prslc.zhiflow.di.appModule
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class Application : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()

        // koin
        startKoin {
            androidContext(this@Application)
            modules(appModule)
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(Client.okHttpClient))
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
}