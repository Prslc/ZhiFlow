package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.BASE_URL
import com.prslc.zhiflow.core.network.HeaderProvider
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.data.model.feedback.NegativeFeedbackPanel
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** Service for the "not interested" panel on recommended feed cards. */
class FeedbackService(private val okHttpClient: OkHttpClient) {

    /**
     * @param contentId The content token carried by the feed card.
     * @param contentType Lowercase as the feed spells it (`answer`); this endpoint wants
     *   it capitalised, unlike the `zrec-feedback` endpoints which take a numeric enum.
     */
    suspend fun getPanel(
        contentId: String,
        contentType: String
    ): Result<NegativeFeedbackPanel> = okHttpClient.safeApiCall {
        val url = "$BASE_URL/negative-feedback/panel".toHttpUrl().newBuilder()
            .addQueryParameter("feed_deliver_type", "Normal")
            .addQueryParameter("content_type", contentType.replaceFirstChar { it.uppercaseChar() })
            .addQueryParameter("content_token", contentId)
            .addQueryParameter("scene_code", "RECOMMEND")
            .addQueryParameter("style", "single")
            .build()

        Request.Builder()
            .url(url)
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
}
