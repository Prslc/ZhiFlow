package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.BASE_URL
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.data.model.feed.ZhihuResponse
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
     * @return A [Result] containing [ZhihuResponse] on success.
     */
    suspend fun getRecommendFeed(
        isRefresh: Boolean = false,
        nextUrl: String? = null
    ): Result<ZhihuResponse> = okHttpClient.safeApiCall {
        val baseUrl = nextUrl.takeUnless { isRefresh || nextUrl == null }
            ?: "${BASE_URL}/topstory/recommend"

        val urlBuilder = baseUrl.toHttpUrl().newBuilder()

        if (isRefresh || nextUrl == null) {
            urlBuilder.addQueryParameter("tsp_ad_cardredesign", "0")
            urlBuilder.addQueryParameter("feed_card_exp", "card_corner|1")
            urlBuilder.addQueryParameter("v_serial", "1")
            urlBuilder.addQueryParameter("isDoubleFlow", "0")
            urlBuilder.addQueryParameter("action", "down")
            urlBuilder.addQueryParameter("refresh_scene", "0")
            urlBuilder.addQueryParameter("scroll", "up")
            urlBuilder.addQueryParameter("limit", "10")
            urlBuilder.addQueryParameter("start_type", "cold")
            urlBuilder.addQueryParameter("device", "phone")
            urlBuilder.addQueryParameter("short_container_setting_value", "0")
            urlBuilder.addQueryParameter("include_guide_relation", "false")
            urlBuilder.addQueryParameter("is_feed_first_request", "0")
        }

        Request.Builder()
            .url(urlBuilder.build())
            .header("x-api-version", "3.1.8")
            .get()
            .build()
    }
}
