package com.prslc.zhiflow.core.network

import com.prslc.zhiflow.core.exception.ApiException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * What every request in the app goes through.
 *
 * The funnel is `safeApiCall` and `safeExecute`, and they are tested here against a real HTTP
 * server rather than a stubbed client: what is worth pinning is the behaviour of the whole path --
 * the status that comes back, the decode that follows it, the exception that reaches the caller --
 * and a stub would be answering for the parts that matter. It also means the requests under test
 * are the ones the services actually build, over the same `apiUrl` and the same `ApiClient`.
 *
 * The two promises every caller leans on: a non-2xx is never reported as a success, and a request
 * that establishes a session does not wait for one -- waiting there would be waiting on the call
 * that would satisfy the wait, while holding the lock that call has to take.
 */
class ApiFunnelTest {

    @Serializable
    private data class Greeting(val name: String)

    /** A gate that says what it was asked, and answers nothing. */
    private class RecordingGate : SessionGate {
        var waits = 0
        val exchanges = mutableListOf<Int>()

        override suspend fun awaitReady() {
            waits++
        }

        override fun onExchange(status: Int) {
            exchanges += status
        }
    }

    private lateinit var server: MockWebServer
    private lateinit var gate: RecordingGate

    /** Where the app aimed, before this test's own interceptor sent it somewhere local. */
    private var aimedAt: HttpUrl? = null

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        gate = RecordingGate()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun funnel(): ApiClient = ApiClient(
        http = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                aimedAt = request.url
                val local = server.url("/")
                chain.proceed(
                    request.newBuilder()
                        .url(
                            request.url.newBuilder()
                                .scheme(local.scheme)
                                .host(local.host)
                                .port(local.port)
                                .build(),
                        )
                        .build(),
                )
            }
            .build(),
        gateProvider = { gate },
    )

    private fun reply(code: Int, body: String = "") {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    private fun greeting(name: String = "Ada") = """{"name":"$name"}"""

    private fun call(path: String = "/greeting"): Result<Greeting> = runBlocking {
        funnel().safeApiCall { Request.Builder().apiUrl(path).get().build() }
    }

    // What comes back

    @Test
    fun `an answered body is decoded and handed back`() {
        reply(200, greeting("Ada"))

        val result = call()

        assertEquals(Greeting("Ada"), result.getOrNull())
    }

    @Test
    fun `the request goes to the API host, by the path the service asked for`() {
        reply(200, greeting())

        call("/search/suggest?q=test")

        assertEquals("api.zhihu.com", aimedAt?.host)
        assertEquals("/search/suggest?q=test", server.takeRequest().target)
    }

    // What is refused

    @Test
    fun `a rejection is a failure, never a success with nothing in it`() {
        reply(500, "")

        val result = call()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApiException.ServerException)
        assertEquals(500, (result.exceptionOrNull() as ApiException.ServerException).code)
    }

    @Test
    fun `a 404 is the not-found failure the screens already draw`() {
        reply(404, "")

        assertTrue(call().exceptionOrNull() is ApiException.NotFoundException)
    }

    @Test
    fun `the account endpoints' own refresh-token code is read as a lost session`() {
        // 400 rather than 401, with a code of its own. Mapped by status alone this would be an
        // ordinary bad request, and the session would never be given up. Nothing is asserted about
        // the wording: a lost session is the one failure this app words for itself rather than
        // passing the server's sentence through.
        reply(400, """{"error":{"code":100008,"message":"refresh token is invalid"}}""")

        assertTrue(call().exceptionOrNull() is ApiException.UnAuthorizedException)
    }

    @Test
    fun `an answer that is not the shape we asked for is a failure, not a crash`() {
        reply(200, "not json at all")

        assertTrue(call().exceptionOrNull() is ApiException.UnknownException)
    }

    @Test
    fun `a server that is not there is the network's failure`() {
        val gone = MockWebServer()
        gone.start()
        val port = gone.port
        gone.close()

        val result = runBlocking {
            ApiClient(http = OkHttpClient.Builder().build(), gateProvider = { gate })
                .safeApiCall<Greeting> {
                    Request.Builder().url("http://127.0.0.1:$port/greeting").get().build()
                }
        }

        assertTrue(result.exceptionOrNull() is ApiException.NetworkException)
    }

    // The write that needs no body

    @Test
    fun `a write is a success when it is answered, and a failure when it is not`() {
        val funnel = funnel()

        reply(200, "")
        assertTrue(runBlocking { funnel.safeExecute { Request.Builder().apiUrl("/vote").post(EMPTY_BODY).build() } }.isSuccess)

        reply(403, "")
        assertTrue(runBlocking { funnel.safeExecute { Request.Builder().apiUrl("/vote").post(EMPTY_BODY).build() } }.isFailure)
    }

    // The session

    @Test
    fun `a request that needs a session waits for one before it goes out`() {
        reply(200, greeting())

        call()

        assertEquals(1, gate.waits)
    }

    @Test
    fun `the call that establishes a session does not wait for the session it is fetching`() {
        // The deadlock this exists to avoid: waiting here waits on the very call that would
        // satisfy the wait, while holding the lock that call has to take.
        reply(200, """{"access_token":"token"}""")

        runBlocking {
            funnel().safeApiCall<Greeting> {
                Request.Builder().apiUrl("/people/self").get().establishesSession().build()
            }
        }

        assertEquals(0, gate.waits)
    }

    @Test
    fun `the gate hears an exchange's outcome, rejection included`() {
        // It renews by watching the answers, and a success counts as much as a rejection.
        val funnel = funnel()

        reply(200, greeting())
        runBlocking { funnel.safeApiCall<Greeting> { Request.Builder().apiUrl("/greeting").get().build() } }
        reply(401, "")
        runBlocking { funnel.safeApiCall<Greeting> { Request.Builder().apiUrl("/greeting").get().build() } }

        assertEquals(listOf(200, 401), gate.exchanges)
    }

    @Test
    fun `a request that never reached the server offers the gate no status at all`() {
        val funnel = funnel()
        server.close()

        runBlocking { funnel.safeApiCall<Greeting> { Request.Builder().apiUrl("/greeting").get().build() } }

        assertTrue("there is no status to report", gate.exchanges.isEmpty())
    }
}

/** A write with nothing to say; the endpoints that matter carry their arguments in the path. */
private val EMPTY_BODY = ByteArray(0).toRequestBody()
