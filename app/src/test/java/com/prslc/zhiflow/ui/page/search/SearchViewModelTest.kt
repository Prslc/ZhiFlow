package com.prslc.zhiflow.ui.page.search

import android.app.Application
import com.prslc.zhiflow.core.network.ApiClient
import com.prslc.zhiflow.core.network.SessionGate
import com.prslc.zhiflow.data.dto.SearchItemDto
import com.prslc.zhiflow.data.model.search.SearchTab
import com.prslc.zhiflow.data.remote.service.SearchService
import com.prslc.zhiflow.data.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The search page's state machine: what the box does while it is being typed into, and what a page
 * of results leaves behind for the next one.
 *
 * The subject is driven through the app's own path -- the ViewModel, the repository, the service
 * and the funnel -- with only the answers written by the test, so what is asserted is what the
 * reader would see rather than what a stub was told to return.
 *
 * The main dispatcher is the real [Dispatchers.Default] and the waits are real time. Virtual time
 * cannot be used here: every call ends in `withContext(Dispatchers.IO)`, and a test scheduler that
 * has advanced past a coroutine suspended on another dispatcher has not waited for it. What that
 * costs is a `Thread.sleep` or two; what it buys is that no assertion is made about a coroutine
 * that has not finished.
 *
 * It runs under Robolectric because the mapper does: a result row's title and excerpt are built by
 * `android.text.Html`, which a plain JVM test cannot call at all.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SearchViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // The far end

    /** A search backend whose replies the test writes, keyed by the url that asked. */
    private class Backend(
        private val delayMs: (HttpUrl) -> Long = { 0 },
        private val reply: (HttpUrl) -> String,
    ) {
        /** Every url the app asked for, in the order the requests went out. */
        val asked = CopyOnWriteArrayList<HttpUrl>()

        /** The same, for the ones that have been answered. */
        val answered = CopyOnWriteArrayList<HttpUrl>()

        val api: ApiClient = ApiClient(
            http = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val url = chain.request().url
                    asked += url
                    delayMs(url).takeIf { it > 0 }?.let { Thread.sleep(it) }
                    answered += url
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(reply(url).toResponseBody(JSON))
                        .build()
                }
                .build(),
            gateProvider = { PASSING },
        )

        fun searching() = asked.filter { it.encodedPath == RESULTS_PATH }

        fun askedFor(query: String) = searching().mapNotNull { it.queryParameter("q") }

        fun suggesting() = asked.count { it.encodedPath == SUGGEST_PATH }

        fun presetWords() = asked.count { it.encodedPath == PRESET_PATH }
    }

    /**
     * A results page, in the shape the endpoint sends one. Every field the app reads is here and
     * nothing else is, so a test that passes says the names line up rather than that the body was
     * long enough to be forgiving.
     */
    private fun results(vararg ids: String, paging: String = """{"is_end":false}"""): String {
        val cards = ids.joinToString(",") { id ->
            """{"type":"search_result","object":{"id":"$id","type":"answer","title":"Answer $id",""" +
                """"author":{"name":"Ada Lovelace"}}}"""
        }
        return """{"data":[$cards],"paging":$paging}"""
    }

    private fun viewModel(backend: Backend) =
        SearchViewModel(SearchRepository(SearchService(backend.api)))

    /** Waits for work that is running on real threads, rather than assuming it has finished. */
    private fun awaitUntil(what: String, check: () -> Boolean) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (check()) return
            Thread.sleep(5)
        }
        fail("waited ${TIMEOUT_MS}ms for $what")
    }

    private fun shownIds(viewModel: SearchViewModel) = viewModel.uiState.items
        .filterIsInstance<SearchItemDto.Content>()
        .map { it.id }

    // What a page leaves behind

    @Test
    fun `a page whose cursor is blank leaves nothing to ask for`() {
        // The server spells "no more pages" two ways, and this is one of them: a `next` that is
        // there and empty. Read as a url it would be a request to nowhere.
        val backend = Backend { results("1", paging = """{"is_end":false,"next":""}""") }
        val viewModel = viewModel(backend)
        viewModel.submit("ada", SearchTab.GENERAL)
        awaitUntil("the page") { viewModel.uiState.items.isNotEmpty() }

        viewModel.loadMore()

        assertEquals(1, backend.searching().size)
    }

    @Test
    fun `a page that says the results end there leaves nothing to ask for either`() {
        val next = "$BASE/search_v3?q=ada&offset=20"
        val backend = Backend { results("1", paging = """{"is_end":true,"next":"$next"}""") }
        val viewModel = viewModel(backend)
        viewModel.submit("ada", SearchTab.GENERAL)
        awaitUntil("the page") { viewModel.uiState.items.isNotEmpty() }

        viewModel.loadMore()

        assertEquals(1, backend.searching().size)
    }

    @Test
    fun `paging asks for the address the server sent, exactly as it sent it`() {
        // That url carries the cursor, the hash id and the tab it was run against. Nothing may be
        // added to it, and `q` is not among the things it carries.
        val next = "$BASE/search_v3?q=ada&t=general&offset=20&search_hash_id=abc"
        val backend = Backend { url ->
            if (url.queryParameter("offset") == "20") {
                results("2", paging = """{"is_end":true}""")
            } else {
                results("1", paging = """{"is_end":false,"next":"$next"}""")
            }
        }
        val viewModel = viewModel(backend)
        viewModel.submit("ada", SearchTab.GENERAL)
        awaitUntil("the first page") { viewModel.uiState.items.isNotEmpty() }

        viewModel.loadMore()
        awaitUntil("the second page") { shownIds(viewModel).size == 2 }

        assertEquals(listOf("1", "2"), shownIds(viewModel))
        val paged = backend.searching().filter { it.queryParameter("offset") == "20" }.single()
        assertEquals(next, paged.toString())
    }

    // The box

    @Test
    fun `a blank submit cannot wipe the results on screen`() {
        val backend = Backend { results("1") }
        val viewModel = viewModel(backend)
        viewModel.submit("ada", SearchTab.GENERAL)
        awaitUntil("the search") { viewModel.uiState.items.isNotEmpty() }

        viewModel.submit("   ", SearchTab.GENERAL)

        assertEquals(1, backend.searching().size)
        assertEquals(listOf("1"), shownIds(viewModel))
        assertEquals("ada", viewModel.submittedQuery)
    }

    @Test
    fun `the phrase is trimmed before it is searched for and put back in the box`() {
        val backend = Backend { results("1") }
        val viewModel = viewModel(backend)

        viewModel.submit("  ada  ", SearchTab.GENERAL)
        awaitUntil("the search") { viewModel.uiState.items.isNotEmpty() }

        assertEquals(listOf("ada"), backend.askedFor("ada"))
        assertEquals("ada", viewModel.query)
        assertEquals("ada", viewModel.submittedQuery)
    }

    @Test
    fun `keystrokes in quick succession ask the server once, for the last of them`() {
        // The box waits out a pause between keystrokes and replaces the request it was going to
        // send. The three here are written in a row, microseconds apart, well inside the wait.
        val backend = Backend { """{"suggest":[{"query":"ada"}]}""" }
        val viewModel = viewModel(backend)

        viewModel.onQueryChange("a")
        viewModel.onQueryChange("ad")
        viewModel.onQueryChange("ada")
        awaitUntil("the suggestions") { viewModel.suggestions.isNotEmpty() }

        assertEquals(1, backend.suggesting())
    }

    @Test
    fun `clearing the box offers nothing and asks for nothing`() {
        val backend = Backend { """{"suggest":[{"query":"ada"}]}""" }
        val viewModel = viewModel(backend)
        viewModel.onQueryChange("ada")
        awaitUntil("the suggestions") { viewModel.suggestions.isNotEmpty() }

        viewModel.onQueryChange("")

        assertTrue(viewModel.suggestions.isEmpty())
        assertEquals(1, backend.suggesting())
    }

    @Test
    fun `a search takes the suggestions out from under the box`() {
        val backend = Backend { url ->
            if (url.encodedPath == SUGGEST_PATH) """{"suggest":[{"query":"ada"}]}""" else results("1")
        }
        val viewModel = viewModel(backend)
        viewModel.onQueryChange("ada")
        awaitUntil("the suggestions") { viewModel.suggestions.isNotEmpty() }

        viewModel.submit("ada", SearchTab.GENERAL)

        assertTrue(viewModel.suggestions.isEmpty())
    }

    @Test
    fun `the narrower phrases arrive under the box`() {
        val backend = Backend { url ->
            when (url.encodedPath) {
                SUGGESTED_PATH -> """{"query_list":[{"query":"tutorial","real_query":"ada tutorial"}]}"""
                else -> results("1")
            }
        }
        val viewModel = viewModel(backend)

        viewModel.submit("ada", SearchTab.GENERAL)
        awaitUntil("the narrower phrases") { viewModel.refinements.isNotEmpty() }

        assertEquals("tutorial", viewModel.refinements.single().suffix)
        assertEquals("ada tutorial", viewModel.refinements.single().query)
    }

    @Test
    fun `clearing the box takes the results and the narrower phrases with it`() {
        val backend = Backend { results("1") }
        val viewModel = viewModel(backend)
        viewModel.submit("ada", SearchTab.GENERAL)
        awaitUntil("the search") { viewModel.uiState.items.isNotEmpty() }

        viewModel.clearResults()

        assertTrue(viewModel.uiState.items.isEmpty())
        assertTrue(viewModel.refinements.isEmpty())
        assertEquals("", viewModel.submittedQuery)
    }

    @Test
    fun `a slower answer for a phrase the reader has left cannot land under the new one`() {
        // Tapping two of the chips under the box does exactly this: two searches go out and the
        // first is answered last. Both write the whole state, so the newer one has to supersede
        // the older rather than race it, or the reader ends up reading the results of a search
        // they have already moved on from while the box names the one they asked for.
        val backend = Backend(
            reply = { url -> results(if (url.queryParameter("q") == "ada") "slow" else "quick") },
            delayMs = { url ->
                if (url.encodedPath == RESULTS_PATH && url.queryParameter("q") == "ada") 400 else 0
            },
        )
        val viewModel = viewModel(backend)

        viewModel.submit("ada", SearchTab.GENERAL)
        // The reader moves on once the first search is already on its way, which is the case the
        // box cannot get out of by simply not sending it.
        awaitUntil("the first search to be on its way") { backend.searching().size == 1 }
        viewModel.submit("grace", SearchTab.GENERAL)
        awaitUntil("both answers") { backend.answered.count { it.encodedPath == RESULTS_PATH } == 2 }
        // The superseded request has been answered; give it the moment it needs to write, if it is
        // going to.
        Thread.sleep(100)

        assertEquals("grace", viewModel.submittedQuery)
        assertEquals(listOf("quick"), shownIds(viewModel))
    }

    // What the page offers before anything is typed

    @Test
    fun `the hot phrases are asked for once, and only by the whole-site tab`() {
        // A page scoped to what the reader has saved would be offering them searches for things
        // they have not.
        val backend = Backend { """{"preset_words":{"words":[{"query":"ada","real_query":"ada"}]}}""" }
        val viewModel = viewModel(backend)

        viewModel.loadHotQueries(SearchTab.FAVLIST)
        assertEquals(0, backend.presetWords())

        viewModel.loadHotQueries(SearchTab.GENERAL)
        awaitUntil("the hot phrases") { viewModel.hotQueries.isNotEmpty() }

        viewModel.loadHotQueries(SearchTab.GENERAL)
        assertEquals(1, backend.presetWords())
    }

    private companion object {
        const val BASE = "https://api.zhihu.com"
        const val RESULTS_PATH = "/search_v3"
        const val SUGGEST_PATH = "/search/suggest"
        const val SUGGESTED_PATH = "/search/suggested_query"
        const val PRESET_PATH = "/search/preset_words"
        const val TIMEOUT_MS = 5_000L
        val JSON = "application/json".toMediaType()

        /** No session to wait for and nothing to tell anyone about: the funnel is not the subject. */
        val PASSING = object : SessionGate {
            override suspend fun awaitReady() = Unit

            override fun onExchange(status: Int) = Unit
        }
    }
}
