package com.prslc.zhiflow.core.network

import com.prslc.zhiflow.BuildConfig
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
                val request = chain.request().newBuilder()
                    .header("Cookie", BuildConfig.cookie)
                    .header("User-Agent", BuildConfig.ua)
                    .header("Authorization", BuildConfig.authorization)
                    .header("x-zse-96", BuildConfig.x_zse_96)
                    .header("x-zse-93", BuildConfig.x_zse_93)
                    .header("x-app-za", HeaderProvider.xAppZa)
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}