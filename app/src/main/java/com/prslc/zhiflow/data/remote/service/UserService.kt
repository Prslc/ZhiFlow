package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.data.model.user.ZhihuUser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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

    /**
     * Follows a user.
     *
     * @param userId The user hash ID (ZhihuUser.id, not url_token).
     * @return A [Result] indicating success or failure.
     */
    suspend fun followUser(userId: String): Result<Boolean> =
        okHttpClient.safeExecute {
            Request.Builder()
                .apiUrl("/people/$userId/followers")
                .post(ByteArray(0).toRequestBody())
                .build()
        }

    /**
     * Unfollows a user.
     *
     * @param userId The user hash ID (ZhihuUser.id, not url_token).
     * @return A [Result] indicating success or failure.
     */
    suspend fun unfollowUser(userId: String): Result<Boolean> =
        okHttpClient.safeExecute {
            Request.Builder()
                .apiUrl("/people/$userId/followers")
                .delete(ByteArray(0).toRequestBody())
                .build()
        }
}
