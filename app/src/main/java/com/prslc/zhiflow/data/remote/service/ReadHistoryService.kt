package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.BASE_URL
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.data.model.user.ReadHistoryResponse
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class ReadHistoryService(private val okHttpClient: OkHttpClient) {

    /**
     * Fetches the user's read history from the unified consumption endpoint.
     *
     * @param offset Pagination offset.
     * @param limit Number of items per page.
     * @return A [Result] containing [ReadHistoryResponse] on success.
     */
    suspend fun getReadHistory(
        offset: Int = 0,
        limit: Int = 10
    ): Result<ReadHistoryResponse> = okHttpClient.safeApiCall {
        val url = "${BASE_URL}/unify-consumption/read_history"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("offset", offset.toString())
            .addQueryParameter("limit", limit.toString())
            .build()

        Request.Builder()
            .url(url)
            .get()
            .build()
    }
}
