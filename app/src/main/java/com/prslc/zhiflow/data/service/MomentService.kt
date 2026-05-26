package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.core.network.Client
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.data.model.MomentsResponse
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Service handling moment-related API requests using OkHttp.
 */
class MomentService(private val okHttpClient: OkHttpClient) {

    /**
     * Fetches the origin content of a moment by user token.
     *
     * @param urlToken The user's unique URL token.
     * @param nextUrl The pagination URL provided by the previous response.
     * @return A [Result] containing [MomentsResponse] on success.
     */
    suspend fun getMoment(
        urlToken: String,
        nextUrl: String? = null
    ): Result<MomentsResponse> = okHttpClient.safeApiCall {
        val url = nextUrl ?: "${Client.BASE_URL}/moments/$urlToken/origin"

        Request.Builder()
            .url(url.toHttpUrl())
            .get()
            .build()
    }
}
