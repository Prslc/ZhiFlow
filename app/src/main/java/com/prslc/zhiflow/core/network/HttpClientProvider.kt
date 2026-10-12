package com.prslc.zhiflow.core.network

import com.prslc.zhiflow.core.network.HeaderProvider.zse96
import kotlinx.serialization.json.Json
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * The clients the app makes requests through, and the headers each request goes out with.
 *
 * Two of them. [okHttpClient] is the API's and carries the session and the signature that host
 * asks for; [imageClient] is everything else's -- Coil's, today -- and carries neither. An image
 * needs no credentials, and running it through the API's client charged every cover and avatar a
 * signature call and a locked read of the credential store, besides letting a picture host's
 * cookies into the session jar.
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
            val builder = original.newBuilder()
                .header("User-Agent", HeaderProvider.UA)
                .header("x-app-version", HeaderProvider.APP_VERSION)
                .header("x-app-za", HeaderProvider.xAppZa)

            // The session and the signature belong to Zhihu's own hosts. What else could arrive
            // here is what a server named: its paging links name those hosts, and so does the
            // eight-digit id of an image, which is why the images take the other client.
            if (original.url.host in CREDENTIALED_HOSTS) {
                val urlPath = original.url.encodedPath +
                    original.url.encodedQuery?.let { "?$it" }.orEmpty()

                val accessToken = credentialStore.read().accessToken
                val auth = if (accessToken.isBlank()) "" else "Bearer $accessToken"
                val xUdid = credentialStore.udid()

                // Left off while there is none, rather than sent empty or made up: the request
                // that asks for this install's first device id is refused outright if it already
                // carries an id the server does not know, so a blank one would be worse than
                // useless. The client sends none on a fresh install for the same reason.
                if (xUdid != null) {
                    builder.header("x-udid", xUdid)
                }

                // Omitted rather than sent empty while there is no session: a bare `Authorization:`
                // is a header the gateway has to parse and cannot, and the client never sends one.
                if (auth.isNotEmpty()) {
                    builder.header("Authorization", auth)
                }

                // A sealed body goes unsigned.
                if (!original.hasSealedBody()) {
                    builder.header("x-zse-96", zse96(urlPath, auth, xUdid.orEmpty()))
                }

                builder.header("x-zse-93", HeaderProvider.ZSE_93)
            }

            chain.proceed(builder.build())
        }
        .addInterceptor(HttpLogInterceptor(httpLogStore))
        .build()

    /**
     * What images are fetched through: the app's own identification and nothing of the session.
     *
     * Its own connection pool and dispatcher rather than the API client's, so a feed's worth of
     * covers cannot queue behind API calls. The device id is left off for the same reason the
     * session is: it identifies this install to Zhihu's API, and a picture host has no business
     * with it.
     */
    val imageClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", HeaderProvider.UA)
                    .header("x-app-version", HeaderProvider.APP_VERSION)
                    .header("x-app-za", HeaderProvider.xAppZa)
                    .build(),
            )
        }
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
