package com.prslc.zhiflow.ui.page.column

import android.app.Application
import com.prslc.zhiflow.core.network.ApiClient
import com.prslc.zhiflow.core.network.CredentialStore
import com.prslc.zhiflow.core.network.Credentials
import com.prslc.zhiflow.core.network.SessionGate
import com.prslc.zhiflow.data.remote.service.ActionService
import com.prslc.zhiflow.data.remote.service.ColumnService
import com.prslc.zhiflow.data.remote.service.UserService
import com.prslc.zhiflow.data.repository.ActionRepository
import com.prslc.zhiflow.data.repository.ColumnRepository
import com.prslc.zhiflow.data.repository.UserRepository
import com.prslc.zhiflow.data.session.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A column's list, and the one thing taking a page of it in has to hold to.
 *
 * The page addresses its pages by counting what it has read itself, and the order of a column is
 * the server's to keep rather than the time a piece was published: one published between two pages
 * slides everything under it down, so the page after the boundary opens on the entry the page
 * before ended with. Both land in the list, and the rows key on the id of the content they name --
 * `LazyColumn` throws when one key is measured twice, which is what the column page did in the
 * reader's hands.
 *
 * What is pinned here is not only that nothing throws. The list has to end up holding the column
 * as it then stood, each entry once, and the offset has to keep counting what was read rather than
 * what survived -- a count taken after the repeat was dropped would skip an entry with every page.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ColumnViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** A column backend whose item pages the test writes, keyed by the offset that asked. */
    private class Backend(
        private val pageOf: (Int) -> String,
    ) {
        val asked = CopyOnWriteArrayList<HttpUrl>()

        val api: ApiClient = ApiClient(
            http = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val url = chain.request().url
                    asked += url
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(answer(url).toResponseBody(JSON))
                        .build()
                }
                .build(),
            gateProvider = { PASSING },
        )

        /** The offsets the item list was asked for, in the order they went out. */
        val offsetsAsked: List<String?>
            get() = asked
                .filter { it.encodedPath.endsWith(ITEMS_PATH) }
                .map { it.queryParameter("offset") }

        private fun answer(url: HttpUrl): String {
            val offset = url.queryParameter("offset")?.toInt() ?: 0

            return when {
                url.encodedPath.endsWith(ITEMS_PATH) -> pageOf(offset)

                url.encodedPath.contains("/columns/v2/") -> COLUMN

                // The visit the page writes on arrival, and whatever else this test is not about.
                else -> "{}"
            }
        }
    }

    /** A store with no session in it. Only the author toggle asks for one, and it stays shut. */
    private class NoCredentials : CredentialStore {

        private val state = MutableStateFlow(Credentials())

        override val session: StateFlow<Credentials> = state

        override fun udid(): String? = null

        override fun replaceUdid(value: String) = Unit

        override fun clearUdid() = Unit

        override fun read(): Credentials = state.value

        override fun save(credentials: Credentials) = Unit

        override fun clear() = Unit
    }

    @Test
    fun `an entry the page before already carried is not drawn a second time`() {
        // One piece is published between the two requests: asked for by offset, the second page
        // (10 onwards, as the column now stands) opens on `a9`, which the first page ended with.
        val beforeThePiece = (0 until 10).map { "a$it" }
        val afterThePiece = (9 until 19).map { "a$it" }
        val backend = Backend { offset -> page(if (offset == 0) beforeThePiece else afterThePiece) }
        val viewModel = viewModel(backend)

        viewModel.loadColumn(COLUMN_TOKEN)
        awaitUntil("the first page") {
            viewModel.uiState.items.isNotEmpty() && !viewModel.uiState.isNextLoading
        }

        viewModel.loadMore()
        awaitUntil("the second page") {
            viewModel.uiState.items.size > beforeThePiece.size && !viewModel.uiState.isNextLoading
        }

        val ids = viewModel.uiState.items.map { it.id }
        assertEquals("one id, one row", ids.size, ids.distinct().size)
        assertEquals(beforeThePiece + afterThePiece.drop(1), ids)
        assertEquals(
            "the offset counts what was read, not what survived the drop",
            listOf("0", "10"),
            backend.offsetsAsked,
        )
    }

    private fun viewModel(backend: Backend): ColumnViewModel {
        // The user repository is here because the page asks for one; nothing in this test reaches
        // the author toggle that is the only thing it serves.
        val userService = UserService(backend.api)
        return ColumnViewModel(
            repository = ColumnRepository(ColumnService(backend.api)),
            userRepository = UserRepository(userService, UserSession(userService, NoCredentials())),
            actionRepository = ActionRepository(ActionService(backend.api)),
        )
    }

    /** A page of the column in the shape the endpoint sends one, with more still to come. */
    private fun page(ids: List<String>): String =
        """{"data":[${ids.joinToString(",") { entry(it) }}],""" +
            """"paging":{"is_end":false,"is_start":false}}"""

    private fun entry(id: String): String =
        """{"id":"$id","title":"A piece","excerpt":"","created":1,"type":"article",""" +
            """"voteup_count":0,"comment_count":0}"""

    private fun awaitUntil(what: String, check: () -> Boolean) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (check()) return
            Thread.sleep(5)
        }
        fail("waited ${TIMEOUT_MS}ms for $what")
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val COLUMN_TOKEN = "c_1867989249418211328"
        const val ITEMS_PATH = "/items"

        val JSON = "application/json".toMediaType()

        /** The column itself, as its own page reads one: only its token and title are drawn on. */
        const val COLUMN =
            """{"url_token":"$COLUMN_TOKEN","title":"A column","intro":"","followers":3,""" +
                """"articles_count":1,"views_count":0,"voteup_count":0,"is_following":false,""" +
                """"author":{"id":"u1","name":"Ada","url_token":"ada","headline":""}}"""

        /**
         * No session to wait for and nothing to tell anyone about: the funnel is not the subject.
         */
        val PASSING = object : SessionGate {
            override suspend fun awaitReady() = Unit

            override fun onExchange(status: Int) = Unit
        }
    }
}
