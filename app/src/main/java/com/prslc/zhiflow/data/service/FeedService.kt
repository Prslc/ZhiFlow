package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.core.network.body
import com.prslc.zhiflow.data.model.ZhihuResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Service handling feed-related API requests using OkHttp.
 */
class FeedService(private val okHttpClient: OkHttpClient) {

    /**
     * Fetches the recommended feed from Zhihu.
     *
     * @param isRefresh Whether this is a pull-to-refresh action.
     * @param nextUrl The pagination URL provided by the previous response.
     * @return A [ZhihuResponse] object or null if the request fails.
     */
    suspend fun getRecommendFeed(
        isRefresh: Boolean = false,
        nextUrl: String? = null
    ): ZhihuResponse? = withContext(Dispatchers.IO) {
        try {
            val baseUrl = nextUrl.takeUnless { isRefresh || nextUrl == null }
                ?: "https://api.zhihu.com/topstory/recommend"

            val urlBuilder = baseUrl.toHttpUrl().newBuilder()

            if (isRefresh || nextUrl == null) {
                urlBuilder.addQueryParameter("action", "down")
                urlBuilder.addQueryParameter("start_type", "cold")
                urlBuilder.addQueryParameter("limit", "10")
                urlBuilder.addQueryParameter("is_feed_first_request", "1")
                urlBuilder.addQueryParameter("refresh_scene", "1")
            }

            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build()
            okHttpClient.newCall(request).execute().body<ZhihuResponse>()
        } catch (e: Exception) {
            null
        }
    }
}