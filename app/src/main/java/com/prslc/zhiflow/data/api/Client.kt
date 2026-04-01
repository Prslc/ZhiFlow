package com.prslc.zhiflow.data.api

import com.prslc.zhiflow.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object Client {
    val jsonInstance = Json {
        ignoreUnknownKeys = true    // Skip undefined fields
        isLenient = true            // Permissive mode
        coerceInputValues = true    // Forced conversion
        encodeDefaults = true       // Default value
    }

    val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(jsonInstance)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
        }

        defaultRequest {
            url("https://api.zhihu.com")
            header(HttpHeaders.Cookie, BuildConfig.cookie)
            header(HttpHeaders.UserAgent, BuildConfig.ua)
            header("x-zse-96", BuildConfig.x_zse_96)
            header("x-zse-93", BuildConfig.x_zse_93)
            header("authorization", BuildConfig.authorization)
        }
    }
}