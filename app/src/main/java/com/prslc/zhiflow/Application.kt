package com.prslc.zhiflow

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.prslc.zhiflow.core.network.Client
import okio.Path.Companion.toPath

class ZhiFlowApplication : Application(), SingletonImageLoader.Factory {

    @OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(Client.client))
                add(AnimatedImageDecoder.Factory())
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizeBytes(64 * 1024 * 1024) // 64MB
                    .build()
            }
            .crossfade(true)
            .build()
    }
}