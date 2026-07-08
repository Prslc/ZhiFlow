package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.data.model.user.ZhihuUser
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Service for retrieving user profile information and social data.
 */
class UserService(private val okHttpClient: OkHttpClient) {

    /**
     * Fetches detailed profile information for a specific user.
     *
     * @param urlToken The unique alphanumeric identifier for a user profile.
     * @return A [Result] containing [ZhihuUser] on success.
     */
    suspend fun getUserDetail(urlToken: String): Result<ZhihuUser> =
        okHttpClient.safeApiCall {
            Request.Builder()
                .apiUrl("/people/$urlToken")
                .get()
                .build()
        }
}
