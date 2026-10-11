package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.ApiClient
import com.prslc.zhiflow.core.network.BASE_URL
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.data.model.search.ZhihuPresetWordsResponse
import com.prslc.zhiflow.data.model.search.ZhihuSearchResponse
import com.prslc.zhiflow.data.model.search.ZhihuSuggestResponse
import com.prslc.zhiflow.data.model.search.ZhihuSuggestedQueryResponse
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Service handling search requests using OkHttp.
 *
 * The whole search runs against `search_v3`, one endpoint shared by every result tab; which kinds
 * of card come back is decided by `t`. The other three calls here feed the search box itself.
 */
class SearchService(private val api: ApiClient) {

    /**
     * Runs a search, or asks for the next page of one.
     *
     * @param query What to search for.
     * @param tab Which result tab to search in. `general` is the one a search opens on.
     * @param nextUrl The page to request, from `paging.next` of a previous response. That url
     *   already carries the cursor, the hash id and the tab it was run against, so nothing is
     *   added to it; null asks for a first page, whose parameters are set here.
     * @return A [Result] holding the page of cards, or the failure that kept them away.
     */
    suspend fun search(
        query: String,
        tab: String = "general",
        nextUrl: String? = null,
    ): Result<ZhihuSearchResponse> = api.safeApiCall {
        val url = nextUrl?.toHttpUrl() ?: "${BASE_URL}/search_v3".toHttpUrl()
        val urlBuilder = url.newBuilder()

        if (nextUrl == null) {
            urlBuilder.addQueryParameter("q", query)
            urlBuilder.addQueryParameter("t", tab)
            urlBuilder.addQueryParameter("search_source", "Normal")
            urlBuilder.addQueryParameter("is_real_time", "0")
            urlBuilder.addQueryParameter("correction", "1")
            urlBuilder.addQueryParameter("show_all_topics", "0")
            urlBuilder.addQueryParameter("pin_flow", "false")
            urlBuilder.addQueryParameter("entry", "main")
            urlBuilder.addQueryParameter("offset", "0")
            urlBuilder.addQueryParameter("limit", "20")
            urlBuilder.addQueryParameter("lc_idx", "0")
            urlBuilder.addQueryParameter("zhida_source", "ai_search_general")
        }

        Request.Builder()
            .url(urlBuilder.build())
            .header("x-api-version", "3.0.91")
            .get()
            .build()
    }

    /**
     * Asks for what the reader might mean, given what they have typed so far.
     *
     * @param query The text so far.
     * @return A [Result] holding the suggestions, at most ten of them.
     */
    suspend fun suggest(query: String): Result<ZhihuSuggestResponse> = api.safeApiCall {
        val url = "${BASE_URL}/search/suggest".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .build()

        Request.Builder()
            .url(url)
            .header("x-api-version", "3.0.91")
            .get()
            .build()
    }

    /**
     * Asks for the narrower phrases that follow a search already run.
     *
     * Each comes back twice: `query` is only the ending, `real_query` the whole phrase to search
     * for next. Sending the ending alone would search for something else entirely.
     *
     * @param query The phrase that was searched for.
     * @return A [Result] holding the follow-up phrases.
     */
    suspend fun suggestedQueries(query: String): Result<ZhihuSuggestedQueryResponse> =
        api.safeApiCall {
            val url = "${BASE_URL}/search/suggested_query".toHttpUrl().newBuilder()
                .addQueryParameter("q", query)
                .build()

            Request.Builder()
                .url(url)
                .header("x-api-version", "3.0.91")
                .get()
                .build()
        }

    /**
     * Asks for the words to offer before anything has been typed.
     *
     * @return A [Result] holding the hot searches, each with the phrase to search for.
     */
    suspend fun presetWords(): Result<ZhihuPresetWordsResponse> = api.safeApiCall {
        val url = "${BASE_URL}/search/preset_words".toHttpUrl().newBuilder()
            .addQueryParameter("is_first", "true")
            .addQueryParameter("is_cold_boot", "false")
            .addQueryParameter("sh_preset", "1")
            .build()

        // The body is a fixed, empty word list; the endpoint takes nothing else for this call.
        val body = """{"w":"[]"}""".toRequestBody("application/json".toMediaType())

        Request.Builder()
            .url(url)
            .header("x-api-version", "3.0.91")
            .post(body)
            .build()
    }
}
