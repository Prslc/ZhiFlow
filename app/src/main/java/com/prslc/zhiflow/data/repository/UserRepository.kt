package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.core.exception.mapOffMain
import com.prslc.zhiflow.data.dto.PersonDto
import com.prslc.zhiflow.data.mapper.toPerson
import com.prslc.zhiflow.data.model.user.PeopleListKind
import com.prslc.zhiflow.data.model.user.ZhihuUser
import com.prslc.zhiflow.data.remote.service.UserService
import com.prslc.zhiflow.data.session.UserSession

/** A page of an account's people: who it follows, or who follows it. */
data class PeoplePageResult(
    val people: List<PersonDto>,
    val nextPageUrl: String?,
    /**
     * Whether that was the last page. The list answers with a `next` even at the end — the cursor
     * is the page after the one asked for, not a promise that there is one — so this is the flag
     * the server sets and not whether a cursor came back.
     */
    val isEnd: Boolean,
)

class UserRepository(
    private val service: UserService,
    private val session: UserSession,
) {

    /**
     * Retrieves a page of one of an account's lists of people.
     *
     * @param kind Which of the two lists to read.
     * @param userId Whose lists to read; null reads the reader's own, which is what needs to know
     *   who they are.
     * @param nextUrl The page to ask for, from a previous result's cursor; null asks for the first.
     *   A cursor carries the list and the account it was run against, so the two above go unread.
     * @return A [Result] holding the people and the cursor for the page after them when there is
     *   one.
     */
    suspend fun people(
        kind: PeopleListKind,
        userId: String? = null,
        nextUrl: String? = null,
    ): Result<PeoplePageResult> {
        val response = if (nextUrl != null) {
            service.peopleByUrl(nextUrl)
        } else {
            val id = userId ?: session.currentUserId().getOrElse { return Result.failure(it) }
            when (kind) {
                PeopleListKind.FOLLOWING -> service.followees(id)
                PeopleListKind.FOLLOWERS -> service.followers(id)
            }
        }

        return response.mapOffMain { body ->
            PeoplePageResult(
                people = body.data.map { it.toPerson() },
                nextPageUrl = body.paging.next?.takeIf { it.isNotBlank() },
                // A page holding nobody ends the list: it advances by cursor, and an empty one has
                // no next cursor to advance to.
                isEnd = body.paging.isEnd || body.data.isEmpty(),
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
