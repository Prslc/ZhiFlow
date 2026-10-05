package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.BASE_URL
import com.prslc.zhiflow.core.network.HeaderProvider
import com.prslc.zhiflow.core.network.HttpClientProvider
import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.data.model.feedback.AggFeedbackRequest
import com.prslc.zhiflow.data.model.feedback.ContentTagList
import com.prslc.zhiflow.data.model.feedback.DELIVER_NORMAL
import com.prslc.zhiflow.data.model.feedback.FeedRootBlock
import com.prslc.zhiflow.data.model.feedback.NegativeFeedbackPanel
import com.prslc.zhiflow.data.model.feedback.SCENE_RECOMMEND
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val TEXT_PLAIN = "text/plain;charset=UTF-8"

/** Service for the "not interested" panel and the block page it leads to. */
class FeedbackService(private val okHttpClient: OkHttpClient) {

    /**
     * Fetches the server-driven "not interested" panel for one piece of content.
     *
     * @param contentId The content token carried by the feed card.
     * @param contentType Lowercase as the feed spells it (`answer`); this endpoint wants
     *   it capitalised, unlike the `zrec-feedback` ones which take a numeric enum.
     * @return A [Result] containing [NegativeFeedbackPanel] on success.
     */
    suspend fun getPanel(
        contentId: String,
        contentType: String
    ): Result<NegativeFeedbackPanel> = okHttpClient.safeApiCall {
        val url = "$BASE_URL/negative-feedback/panel".toHttpUrl().newBuilder()
            .addQueryParameter("feed_deliver_type", DELIVER_NORMAL)
            .addQueryParameter("content_type", contentType.replaceFirstChar { it.uppercaseChar() })
            .addQueryParameter("content_token", contentId)
            .addQueryParameter("scene_code", SCENE_RECOMMEND)
            .addQueryParameter("style", "single")
            .build()

        Request.Builder()
            .url(url)
            .header("x-api-version", HeaderProvider.API_VERSION)
            .get()
            .build()
    }

    /**
     * Fetches the topics this content can be filtered by, for the block page.
     *
     * @param contentToken The content token carried by the feed card.
     * @param contentType The numeric enum the panel's `zhihu://` link carries.
     * @param feedbackType The feedback type from the same link, e.g. `LESS_RECOMMEND`.
     * @return A [Result] containing [ContentTagList] on success.
     */
    suspend fun getContentTags(
        contentToken: String,
        contentType: String,
        feedbackType: String
    ): Result<ContentTagList> = okHttpClient.safeApiCall {
        Request.Builder()
            .url(blockUrl("/zrec-feedback/content-tags", contentToken, contentType, feedbackType))
            .header("x-api-version", HeaderProvider.API_VERSION)
            .get()
            .build()
    }

    /**
     * Fetches the keywords already blocked, along with the length and count limits the
     * block page has to enforce.
     *
     * @return A [Result] containing [FeedRootBlock] on success.
     */
    suspend fun getBlockKeywords(
        contentToken: String,
        contentType: String,
        feedbackType: String
    ): Result<FeedRootBlock> = okHttpClient.safeApiCall {
        Request.Builder()
            .url(blockUrl("/feed-root/block", contentToken, contentType, feedbackType))
            .header("x-api-version", HeaderProvider.API_VERSION)
            .get()
            .build()
    }

    /**
     * Runs a request row of the panel.
     *
     * @param url The `backend_url` that came with the row; every parameter already sits in it.
     * @param method The HTTP method that came with the same row.
     * @return A [Result] indicating success or failure.
     */
    suspend fun submit(url: String, method: String): Result<Unit> = okHttpClient.safeExecute {
        Request.Builder()
            .url(url)
            .method(method, "".toRequestBody(null))
            .build()
    }

    /**
     * Submits the block page's selection.
     *
     * @param request The topics and keywords currently on screen. The body carries the whole
     *   page state, so its keyword list replaces what the server holds rather than adding to it.
     * @return A [Result] indicating success or failure.
     */
    suspend fun submitBlockFeedback(request: AggFeedbackRequest): Result<Unit> =
        okHttpClient.safeExecute {
            val body = HttpClientProvider.jsonInstance.encodeToString(request)
                .toRequestBody(TEXT_PLAIN.toMediaType())

            Request.Builder()
                .apiUrl("/zrec-feedback/content/sub/agg/feedback")
                .header("x-api-version", HeaderProvider.API_VERSION)
                .post(body)
                .build()
        }

    private fun blockUrl(
        path: String,
        contentToken: String,
        contentType: String,
        feedbackType: String
    ): HttpUrl = "$BASE_URL$path".toHttpUrl().newBuilder()
        .addQueryParameter("content_type", contentType)
        .addQueryParameter("content_token", contentToken)
        .addQueryParameter("scene_code", SCENE_RECOMMEND)
        .addQueryParameter("feedback_type", feedbackType)
        .addQueryParameter("feed_deliver_type", DELIVER_NORMAL)
        .build()
}
