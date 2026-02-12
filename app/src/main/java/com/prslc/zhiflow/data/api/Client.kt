package com.prslc.zhiflow.data.api

import com.prslc.zhiflow.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object Client {
    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // Skip undefined fields
                isLenient = true         // Permissive mode
                coerceInputValues = true // Forced conversion
            })
        }

        // Timeout
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
        }

        // Request
        defaultRequest {
            url("https://www.zhihu.com/api/v3/")
            header(HttpHeaders.Cookie, BuildConfig.COOKIE)
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            header(HttpHeaders.Accept, "application/json")
        }
    }
}