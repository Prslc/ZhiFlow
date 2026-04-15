package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.ZhihuUser
import com.prslc.zhiflow.data.service.UserService

class UserRepository(private val service: UserService) {

    /**
     * Fetch current user profile details
     *
     * @return A [Result] containing [ZhihuUser] on success, or an exception if the data is null or request fails
     */
    suspend fun getUserDetail(): Result<ZhihuUser> {
        return try {
            val data = service.getUserDetail()
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