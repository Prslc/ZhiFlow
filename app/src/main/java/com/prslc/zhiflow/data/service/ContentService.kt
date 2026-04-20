package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.core.network.body
import com.prslc.zhiflow.data.model.ZhihuAnswer
import com.prslc.zhiflow.data.model.ZhihuArticle
import com.prslc.zhiflow.data.model.ZhihuContent
import com.prslc.zhiflow.data.model.ZhihuPin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Service for fetching detailed information for various types of Zhihu content.
 *
 * This service leverages reified generics to handle polymorphic content types
 * like Answers, Articles, and Pins within a unified fetch logic.
 */
class ContentService(private val okHttpClient: OkHttpClient) {

    /**
     * Generic internal helper to fetch content by its ID and path.
     *
     * @param T The content model extending [ZhihuContent].
     * @param path The API endpoint prefix (e.g., "answers").
     * @param id The unique identifier of the content.
     * @return The deserialized content object or null on failure.
     */
    private suspend inline fun <reified T : ZhihuContent> fetchContent(
        path: String,
        id: String
    ): T? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.zhihu.com/$path/v2/$id"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            okHttpClient.newCall(request).execute().body<T>()
        } catch (e: Exception) {
            null
        }
    }

    /** Fetches details for a Zhihu Answer. */
    suspend fun getAnswerDetail(id: String) = fetchContent<ZhihuAnswer>("answers", id)

    /** Fetches details for a Zhihu Article. */
    suspend fun getArticleDetail(id: String) = fetchContent<ZhihuArticle>("articles", id)

    /** Fetches details for a Zhihu Pin (Thought). */
    suspend fun getPinDetail(id: String) = fetchContent<ZhihuPin>("pins", id)
}