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
     * Two groups of parameters travel with it. The client's own state -- direction, device, the
     * counters -- goes on every request, a whole page url the server handed back included. What
     * asks for one particular page, its experiment flags and its size, goes only on a first page
     * this builds itself: a url from the server already spells out its own cursor.
     *
     * @param isColdStart True only for the first page a session asks for (`action=down`,
     *   `start_type=cold`). Everything after it, a pull-to-refresh included, is `warm`.
     * @param nextUrl The page to request, taken from a previous response: `paging.previous` when
     *   refreshing, `paging.next` when paging. Null asks for the first page.
     * @return A [Result] containing [ZhihuResponse] on success.
     */
    suspend fun getRecommendFeed(
        isColdStart: Boolean = false,
        nextUrl: String? = null
    ): Result<ZhihuResponse> = okHttpClient.safeApiCall {
        val urlBuilder = (nextUrl ?: "${BASE_URL}/topstory/recommend").toHttpUrl().newBuilder()

        urlBuilder.addQueryParameter("refresh_scene", "0")
        urlBuilder.addQueryParameter("start_type", if (isColdStart) "cold" else "warm")
        urlBuilder.addQueryParameter("device", "phone")
        urlBuilder.addQueryParameter("short_container_setting_value", "0")
        urlBuilder.addQueryParameter("include_guide_relation", "false")
        urlBuilder.addQueryParameter("is_feed_first_request", "0")

        if (nextUrl == null) {
            urlBuilder.addQueryParameter("tsp_ad_cardredesign", "0")
            urlBuilder.addQueryParameter("feed_card_exp", "card_corner|1")
            urlBuilder.addQueryParameter("v_serial", "1")
            urlBuilder.addQueryParameter("isDoubleFlow", "0")
            urlBuilder.addQueryParameter("action", if (isColdStart) "down" else "pull")
            urlBuilder.addQueryParameter("scroll", "up")
            urlBuilder.addQueryParameter("limit", "10")
        }

        Request.Builder()
            .url(urlBuilder.build())
            .header("x-api-version", "3.1.8")
            .get()
            .build()
    }
}
