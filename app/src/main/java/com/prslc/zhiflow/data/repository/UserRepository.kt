package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.core.exception.mapOffMain
import com.prslc.zhiflow.data.dto.PersonDto
import com.prslc.zhiflow.data.mapper.toPerson
import com.prslc.zhiflow.data.model.user.ZhihuUser
import com.prslc.zhiflow.data.remote.service.UserService
import com.prslc.zhiflow.data.session.UserSession

/** A page of the people the reader follows. */
data class FolloweesResult(
    val people: List<PersonDto>,
    val nextPageUrl: String?,
)

class UserRepository(
    private val service: UserService,
    private val session: UserSession,
) {

    /**
     * Retrieves a page of the people the reader follows.
     *
     * @param nextUrl The page to ask for, from a previous result's cursor; null asks for the first
     *   one, which is the reader's own list and so needs to know who they are.
     * @return A [Result] holding the people and the cursor for the page after them when there is
     *   one.
     */
    suspend fun followees(nextUrl: String? = null): Result<FolloweesResult> {
        val response = if (nextUrl != null) {
            service.followeesByUrl(nextUrl)
        } else {
            val userId = session.currentUserId().getOrElse { return Result.failure(it) }
            service.followees(userId)
        }

        return response.mapOffMain { body ->
            FolloweesResult(
                people = body.data.map { it.toPerson() },
                nextPageUrl = body.paging.next.takeIf { it.isNotBlank() },
            )
        }
    }

    /**
     * Fetch current user profile details. The response also seeds [UserSession],
     * so pages that need the current user's id need no second lookup.
     *
     * @return A [Result] containing [ZhihuUser] on success
     */
    suspend fun getMyDetail(): Result<ZhihuUser> = service.getUserDetail("self")

    /**
     * Fetch the anonymous session's own profile, for the default avatar it carries.
     *
     * @return A [Result] containing [ZhihuUser] on success
     */
    suspend fun getGuestDetail(): Result<ZhihuUser> = service.getGuestSelf()

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
