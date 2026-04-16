package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.ZhihuUser
import com.prslc.zhiflow.data.service.UserService

class UserRepository(private val service: UserService) {

    /**
     * Fetch current user profile details
     *
     * @return A [Result] containing [ZhihuUser] on success, or an exception if the data is null or request fails
     */
    suspend fun getMyDetail(): Result<ZhihuUser> {
        return try {
            val data = service.getUserDetail("self")
            if (data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the public profile details of a specific user.
     *
     * @param urlToken The unique identifier (slug) of the user (e.g., "excited-vczh").
     * @return A [Result] wrapping [ZhihuUser]. Returns [Result.failure] if the user is not found or request fails.
     */
    suspend fun getUserDetail(urlToken: String): Result<ZhihuUser> {
        return try {
            val data = service.getUserDetail(urlToken)
            if (data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}