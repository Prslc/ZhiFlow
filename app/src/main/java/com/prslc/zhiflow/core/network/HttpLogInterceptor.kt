package com.prslc.zhiflow.core.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

private const val MAX_REQUEST_BODY_BYTES = 4 * 1024L
private const val MAX_RESPONSE_BODY_BYTES = 16 * 1024L

/**
 * Endpoints whose response body hands over a credential in the clear.
 *
 * A body is read for every other endpoint; request bodies need no such list, because the ones
 * that carry anything sensitive are sealed before they leave.
 */
private val CREDENTIAL_BODY_PATHS = setOf(
    "/captcha",
    "/api/account/prod/sign_in",
    "/api/account/prod/guests/token",
    "/api/account/prod/client_logout",
)

/**
 * Records Zhihu API traffic into [HttpLogStore] for the in-app log screen.
 *
 * Hand-rolled rather than OkHttp's `HttpLoggingInterceptor` because the screen wants
 * structured entries instead of line-oriented text, and nothing may reach logcat.
 * Installed after the auth interceptor, so it observes the request as it goes out.
 * Headers — Cookie, Authorization and the signing headers — are never recorded, and neither is
 * the body of an endpoint listed in [CREDENTIAL_BODY_PATHS]: the entry still says the call
 * happened and what it answered, which is what the screen is for, without the token it carried.
 */
class HttpLogInterceptor(private val store: HttpLogStore) : Interceptor {

    private val nextId = AtomicLong()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // Only Zhihu's own hosts are API traffic: images go through another client by now, and
        // anything else on this one is filtered out before any body buffering happens.
        if (request.url.host !in CREDENTIALED_HOSTS) return chain.proceed(request)

        val (requestBody, requestTruncated) = request.bodyText()
        val startedAt = System.nanoTime()

        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            record(
                request = request,
                requestBody = requestBody,
                startedAt = startedAt,
                status = null,
                responseBody = null,
                truncated = requestTruncated,
                error = "${e.javaClass.simpleName}: ${e.message.orEmpty()}",
            )
            throw e
        }

        // Failures are always worth recording; successes only while capture is on.
        if (store.isEnabled || !response.isSuccessful) {
            val peeked = if (request.url.encodedPath in CREDENTIAL_BODY_PATHS) {
                null
            } else {
                runCatching {
                    val body = response.peekBody(MAX_RESPONSE_BODY_BYTES)
                    val truncated = body.contentLength() >= MAX_RESPONSE_BODY_BYTES
                    body.string().takeIf { it.isNotEmpty() } to truncated
                }.getOrNull()
            }

            record(
                request = request,
                requestBody = requestBody,
                startedAt = startedAt,
                status = response.code,
                responseBody = peeked?.first,
                truncated = requestTruncated || (peeked?.second ?: false),
                error = null,
            )
        }

        return response
    }

    private fun record(
        request: Request,
        requestBody: String?,
        startedAt: Long,
        status: Int?,
        responseBody: String?,
        truncated: Boolean,
        error: String?,
    ) {
        // Logging must never be able to decide whether a request succeeds.
        runCatching {
            store.add(
                HttpLogEntry(
                    id = nextId.incrementAndGet(),
                    at = System.currentTimeMillis(),
                    method = request.method,
                    url = request.url.toString(),
                    status = status,
                    durationMs = (System.nanoTime() - startedAt) / 1_000_000,
                    requestBody = requestBody,
                    responseBody = responseBody,
                    error = error,
                    truncated = truncated,
                ),
            )
        }
    }
}

private fun Request.bodyText(): Pair<String?, Boolean> {
    val body = body ?: return null to false
    if (body.isDuplex() || body.isOneShot()) return null to false

    val buffer = Buffer()
    if (runCatching { body.writeTo(buffer) }.isFailure || buffer.size == 0L) return null to false

    val truncated = buffer.size > MAX_REQUEST_BODY_BYTES
    val bytes = buffer.readByteArray(minOf(buffer.size, MAX_REQUEST_BODY_BYTES))
    return bytes.toString(body.contentType()?.charset() ?: Charsets.UTF_8) to truncated
}
