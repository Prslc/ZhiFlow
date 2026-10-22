package com.prslc.zhiflow.ui.page.comment

import android.app.Application
import androidx.compose.ui.text.AnnotatedString
import com.prslc.zhiflow.core.network.ApiClient
import com.prslc.zhiflow.core.network.SessionGate
import com.prslc.zhiflow.data.dto.CommentDto
import com.prslc.zhiflow.data.model.comment.CommentAuthor
import com.prslc.zhiflow.data.model.comment.CommentContent
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.remote.service.CommentService
import com.prslc.zhiflow.data.repository.CommentRepository
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
 * The comment sheet's two panes that are *about* something.
 *
 * The list of root comments is about the content the sheet was opened on, and that does not change
 * while the screen lives. The replies are about one comment, and the passage is about one range in
 * the body: the reader can move from either to another while the first one's page is still out, and
 * both write the same state. So the newer one has to supersede the older rather than race it, or the
 * sheet shows one comment's replies under another's name.
 *
 * A framework test, though nothing in this file says so: the path runs through the mapper into the
 * parser, and `CommentParser` reads a comment's markup with `android.text.Html`. Reading the
 * mapper alone answers "no framework here" and is wrong.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CommentViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Default)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** A comment backend whose replies the test writes, keyed by the url that asked. */
    private class Backend(
        private val delayMs: (HttpUrl) -> Long = { 0 },
        private val reply: (HttpUrl) -> String,
    ) {
        val asked = CopyOnWriteArrayList<HttpUrl>()
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
    }

    /** A page of one comment, in the shape the endpoint sends one. */
    private fun page(marker: String): String =
        """{"data":[{"id":"$marker-1","content":"a reply","author":{"id":"u1","name":"Ada"},""" +
            """"created_time":1,"like_count":0,"child_comment_count":0}],""" +
            """"counts":{"total_counts":1},"paging":{"is_end":true}}"""

    private fun comment(id: String) = CommentDto(
        id = id,
        author = CommentAuthor(id = "u1", name = "Ada"),
        replyToAuthor = null,
        authorTags = emptyList(),
        createdTime = 1,
        likeCount = 0,
        liked = false,
        disliked = false,
        isAuthorTop = false,
        childCount = 0,
        childComments = emptyList(),
        ipInfo = null,
        parsedContent = CommentContent(AnnotatedString(""), emptyList()),
        canReply = true,
        canLike = true,
        canDislike = true,
    )

    private fun viewModel(backend: Backend) =
        CommentViewModel(CommentRepository(CommentService(backend.api)))

    private fun awaitUntil(what: String, check: () -> Boolean) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (check()) return
            Thread.sleep(5)
        }
        fail("waited ${TIMEOUT_MS}ms for $what")
    }

    private fun Backend.repliesFor(id: String) = asked.any { it.encodedPath.contains("/comment/$id/") }

    private fun Backend.answersFor(id: String) = answered.count { it.encodedPath.contains("/comment/$id/") }

    @Test
    fun `an answer for replies the reader has left cannot land under the comment they opened next`() {
        // Tapping from one comment's replies to another's does this: the first request is already
        // out when the second one's state has replaced the pane's.
        val backend = Backend(
            delayMs = { url -> if (url.encodedPath.contains("/comment/A/")) 400 else 0 },
            reply = { url -> page(if (url.encodedPath.contains("/comment/A/")) "A" else "B") },
        )
        val viewModel = viewModel(backend)

        viewModel.loadChildComments(comment("A"), forceRefresh = true)
        awaitUntil("the first request to be on its way") { backend.repliesFor("A") }
        viewModel.loadChildComments(comment("B"), forceRefresh = true)
        awaitUntil("both answers") { backend.answersFor("A") + backend.answersFor("B") == 2 }
        // The superseded request has been answered; give it a moment to write, if it will.
        Thread.sleep(100)

        assertEquals("B", viewModel.childUiState.rootComment?.comment?.id)
        assertEquals(listOf("B-1"), viewModel.childUiState.comments.map { it.comment.id })
    }

    @Test
    fun `an answer for a passage the reader has left cannot land under the one they tapped next`() {
        val backend = Backend(
            delayMs = { url -> if (url.queryParameter("segment_id") == "A") 400 else 0 },
            reply = { url -> page(url.queryParameter("segment_id").orEmpty()) },
        )
        val viewModel = viewModel(backend)

        viewModel.loadSegmentComments("734558056", ContentType.ANSWER, "A", forceRefresh = true)
        awaitUntil("the first request to be on its way") {
            backend.asked.any { it.queryParameter("segment_id") == "A" }
        }
        viewModel.loadSegmentComments("734558056", ContentType.ANSWER, "B", forceRefresh = true)
        awaitUntil("both answers") {
            backend.answered.count { it.queryParameter("segment_id") != null } == 2
        }
        Thread.sleep(100)

        assertEquals(listOf("B-1"), viewModel.segmentUiState.comments.map { it.comment.id })
        assertTrue(
            "the count beside it is the passage on screen, not the one left behind",
            viewModel.segmentUiState.comments.none { it.comment.id.startsWith("A") },
        )
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        val JSON = "application/json".toMediaType()

        /** No session to wait for and nothing to tell anyone about: the funnel is not the subject. */
        val PASSING = object : SessionGate {
            override suspend fun awaitReady() = Unit

            override fun onExchange(status: Int) = Unit
        }
    }
}
