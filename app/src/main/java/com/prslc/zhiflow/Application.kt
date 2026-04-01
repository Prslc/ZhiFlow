package com.prslc.zhiflow

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import coil3.gif.AnimatedImageDecoder
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.prslc.zhiflow.data.api.Client

class ZhiFlowApplication : Application(), SingletonImageLoader.Factory {

    @OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(Client.client))
                add(AnimatedImageDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}