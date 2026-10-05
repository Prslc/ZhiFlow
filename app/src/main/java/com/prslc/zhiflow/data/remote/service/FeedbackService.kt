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
     * @param contentId The content token carried by the feed card.
     * @param contentType Lowercase as the feed spells it (`answer`); this endpoint wants
     *   it capitalised, unlike the `zrec-feedback` ones which take a numeric enum.
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

    /** @param contentType The numeric enum the panel's `zhihu://` link carries. */
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

    /** Every parameter already sits in the URL the backend handed out. */
    suspend fun submit(url: String, method: String): Result<Unit> = okHttpClient.safeExecute {
        Request.Builder()
            .url(url)
            .method(method, "".toRequestBody(null))
            .build()
    }

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
