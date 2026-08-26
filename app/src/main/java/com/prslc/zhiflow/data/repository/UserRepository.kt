package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.user.ZhihuUser
import com.prslc.zhiflow.data.remote.service.UserService

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

    /**
     * Follows a user.
     *
     * @param userId The user hash ID (ZhihuUser.id).
     * @return A [Result] indicating success or failure.
     */
    suspend fun followUser(userId: String): Result<Boolean> = service.followUser(userId)

    /**
     * Unfollows a user.
     *
     * @param userId The user hash ID (ZhihuUser.id).
     * @return A [Result] indicating success or failure.
     */
    suspend fun unfollowUser(userId: String): Result<Boolean> = service.unfollowUser(userId)
}
