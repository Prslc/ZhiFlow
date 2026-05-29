package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.ZhihuUser
import com.prslc.zhiflow.data.service.UserService

class UserRepository(private val service: UserService) {

    /**
     * Fetch current user profile details
     *
     * @return A [Result] containing [ZhihuUser] on success
     */
    suspend fun getMyDetail(): Result<ZhihuUser> = service.getUserDetail("self")

    /**
     * Fetch the public profile details of a specific user.
     *
     * @param urlToken The unique identifier (slug) of the user (e.g., "excited-vczh").
     * @return A [Result] wrapping [ZhihuUser].
     */
    suspend fun getUserDetail(urlToken: String): Result<ZhihuUser> =
        service.getUserDetail(urlToken)
}