package com.prslc.zhiflow.core.exception

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

/**
 * What a thrown thing turns into.
 *
 * This is the funnel's last step and the only one the reader ever sees, and every branch of it is
 * silent: a status that maps to the wrong exception is a screen that says the wrong thing, and a
 * `CancellationException` that gets mapped is an error state raised for a request that was merely
 * superseded. The `ApiException`s are plain classes rather than data classes, so each assertion
 * here is about the *kind* it landed in and what it carries, not about equality.
 */
class FailureMappingTest {

    private fun response(code: Int) = Response.Builder()
        .request(Request.Builder().url("https://api.zhihu.com/x").build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("Rejected")
        .build()

    private fun rejected(code: Int, apiErrorCode: Int? = null, message: String? = null) =
        HttpStatusException(response(code), apiErrorCode, message)

    @Test
    fun `a transport failure is the network's`() {
        assertTrue(IOException("no route").toApiException() is ApiException.NetworkException)
        assertTrue(SocketTimeoutException("timed out").toApiException() is ApiException.NetworkException)
    }

    @Test
    fun `a credential the server will not take is unauthorized`() {
        assertTrue(rejected(401).toApiException() is ApiException.UnAuthorizedException)
        assertTrue(rejected(403).toApiException() is ApiException.UnAuthorizedException)
    }

    @Test
    fun `the account endpoints' own rejection of a refresh token is not read as a bad request`() {
        // A finished credential can arrive as 400 with a code of its own; missing it is silent, and
        // the reader is asked to sign in again and again.
        val failure = rejected(400, apiErrorCode = 100008, message = "invalid refresh token")
            .toApiException()

        assertTrue(failure is ApiException.UnAuthorizedException)
    }

    @Test
    fun `a missing thing is the not-found one`() {
        assertTrue(rejected(404).toApiException() is ApiException.NotFoundException)
    }

    @Test
    fun `every other status is a server failure carrying it`() {
        val tooMany = rejected(429, message = "slow down").toApiException()
        assertTrue(tooMany is ApiException.ServerException)
        assertEquals(429, (tooMany as ApiException.ServerException).code)

        val broken = rejected(503).toApiException()
        assertEquals(503, (broken as ApiException.ServerException).code)
    }

    @Test
    fun `the server's own wording travels with the failure`() {
        // Preferred over the resource string, which can only name the status code.
        val failure = rejected(429, message = "too many requests").toApiException()

        assertEquals("too many requests", failure.serverMessage)
    }

    @Test
    fun `a failure with no envelope to read is still a server failure`() {
        val failure = rejected(400).toApiException()

        assertTrue(failure is ApiException.ServerException)
        assertEquals(null, failure.serverMessage)
    }

    @Test
    fun `a status outside every range is unknown`() {
        assertTrue(rejected(100).toApiException() is ApiException.UnknownException)
        assertTrue(rejected(600).toApiException() is ApiException.UnknownException)
    }

    @Test
    fun `a throwable the app cannot name is still a failure it can report`() {
        assertTrue(IllegalStateException("boom").toApiException() is ApiException.UnknownException)
    }

    @Test
    fun `a failure that is already an ApiException is passed along, not re-wrapped`() {
        val already = ApiException.NotFoundException()

        assertSame(already, already.toApiException())
    }

    @Test
    fun `cancellation is control flow, not a failure to map`() {
        val cancelled = CancellationException("superseded")

        val thrown = runCatching { cancelled.toApiException() }.exceptionOrNull()

        assertSame(cancelled, thrown)
    }
}
