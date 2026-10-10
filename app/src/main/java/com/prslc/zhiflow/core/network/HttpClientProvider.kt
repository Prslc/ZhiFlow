package com.prslc.zhiflow.core.network

import com.prslc.zhiflow.core.network.HeaderProvider.zse96
import kotlinx.serialization.json.Json
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * The one OkHttp client every service shares, with the headers each request goes out with.
 *
 * Header order here is incidental; what is not is that a `Cookie` header is never written. The
 * jar owns those, and OkHttp replaces an explicitly set header with what the jar returns, so a
 * second source would be a way to be silently wrong rather than a belt to go with braces.
 */
class HttpClientProvider(
    private val credentialStore: CredentialStore,
    cookieJar: CookieJar,
    httpLogStore: HttpLogStore,
) {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .cookieJar(cookieJar)
        .addInterceptor { chain ->
            val original = chain.request()
            val urlPath = original.url.encodedPath + original.url.encodedQuery?.let { "?$it" }.orEmpty()

            val accessToken = credentialStore.read().accessToken
            val auth = if (accessToken.isBlank()) "" else "Bearer $accessToken"
            val xUdid = credentialStore.udid()

            val builder = original.newBuilder()
                .header("User-Agent", HeaderProvider.UA)
                .header("x-app-version", HeaderProvider.APP_VERSION)
                .header("x-app-za", HeaderProvider.xAppZa)
                .header("x-udid", xUdid)

            // Omitted rather than sent empty while there is no session: a bare `Authorization:`
            // is a header the gateway has to parse and cannot, and the client never sends one.
            if (auth.isNotEmpty()) {
                builder.header("Authorization", auth)
            }

            // A sealed body goes unsigned: those endpoints take no x-zse-96 either.
            if (!original.hasSealedBody()) {
                builder.header("x-zse-96", zse96(urlPath, auth, xUdid))
            }

            val request = builder
                .header("x-zse-93", HeaderProvider.ZSE_93)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLogInterceptor(httpLogStore))
        .build()

    companion object {
        val jsonInstance = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            encodeDefaults = true
        }
    }
}
