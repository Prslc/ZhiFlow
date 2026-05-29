package com.prslc.zhiflow.core.network

import com.prslc.zhiflow.BuildConfig
import com.prslc.zhiflow.core.network.HeaderProvider.zse96
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object Client {
    const val BASE_URL = "https://api.zhihu.com"

    val jsonInstance = Json {
        ignoreUnknownKeys = true    // Skip undefined fields
        isLenient = true            // Permissive mode
        coerceInputValues = true    // Forced conversion
        encodeDefaults = true       // Default value
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val urlPath = original.url.encodedPath + original.url.encodedQuery?.let { "?$it" }.orEmpty()
                val auth = BuildConfig.authorization

                val request = original.newBuilder()
                    .header("User-Agent", BuildConfig.ua)
                    .header("x-app-version", HeaderProvider.APP_VERSION)
                    .header("x-app-za", HeaderProvider.xAppZa)
                    .header("x-udid", BuildConfig.x_udid)
                    .header("Cookie", BuildConfig.cookie)
                    .header("Authorization", auth)
                    .header("x-zse-96", zse96(auth, urlPath))
                    .header("x-zse-93", HeaderProvider.ZSE_93)
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}
