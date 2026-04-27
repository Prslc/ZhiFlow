package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.body
import com.prslc.zhiflow.data.model.ZhihuUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
     * @return A [ZhihuUser] object containing biography, followers, etc., or null on failure.
     */
    suspend fun getUserDetail(urlToken: String): ZhihuUser? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .apiUrl("/people/$urlToken")
                .get()
                .build()

            okHttpClient.newCall(request).execute().body<ZhihuUser>()
        } catch (e: Exception) {
            // Failure usually implies 404 (user not found) or 403 (blocked)
            null
        }
    }
}