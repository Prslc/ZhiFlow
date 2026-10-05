package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.user.ZhihuUser
import com.prslc.zhiflow.data.remote.service.UserService
import com.prslc.zhiflow.data.session.UserSession

class UserRepository(
    private val service: UserService,
    private val session: UserSession,
) {

    /**
     * Fetch current user profile details. The response also seeds [UserSession],
     * so pages that need the current user's id need no second lookup.
     *
     * @return A [Result] containing [ZhihuUser] on success
     */
    suspend fun getMyDetail(): Result<ZhihuUser> =
        service.getUserDetail("self").onSuccess { session.prime(it.id) }

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
    suspend fun followUser(userId: String): Result<Unit> = service.followUser(userId)

    /**
     * Unfollows a user.
     *
     * @param userId The TARGET user's hash ID (ZhihuUser.id, not url_token).
     *               The current user's hash ID, which the endpoint expects as the
     *               trailing URL segment, is resolved via [UserSession].
     *
     * @return A [Result] indicating success or failure.
     */
    suspend fun unfollowUser(userId: String): Result<Unit> {
        val currentUserId = session.currentUserId()
            .getOrElse { return Result.failure(it) }
        return service.unfollowUser(userId, currentUserId)
    }
}
