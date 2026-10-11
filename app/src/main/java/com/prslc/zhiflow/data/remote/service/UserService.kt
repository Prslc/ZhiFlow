package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.ApiClient
import com.prslc.zhiflow.core.network.BASE_URL
import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.data.model.user.ZhihuFolloweesResponse
import com.prslc.zhiflow.data.model.user.ZhihuUser
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Service for retrieving user profile information and social data.
 */
class UserService(private val api: ApiClient) {

    /**
     * Fetches detailed profile information for a specific user.
     *
     * Profile v2 is the only route that fills in a server-side default cover.
     * `self` has no v2 route (`/people/self/profile` is 404), but `/people/self`
     * returns the self model, which carries `cover_url` too.
     *
     * @param urlToken The unique alphanumeric identifier for a user profile.
     * @return A [Result] containing [ZhihuUser] on success.
     */
    suspend fun getUserDetail(urlToken: String): Result<ZhihuUser> =
        api.safeApiCall {
            Request.Builder()
                .apiUrl(
                    if (urlToken == SELF_TOKEN) {
                        "/people/$urlToken"
                    } else {
                        "/people/$urlToken/profile?profile_new_version=1"
                    },
                )
                .get()
                .build()
        }

    /**
     * Fetches the anonymous session's own profile.
     *
     * `people/self` is not answered for an anonymous session; this is the route that is. What
     * comes back is empty but for an id and an avatar, and that avatar is the server's own
     * default — the only official picture of an anonymous reader there is.
     *
     * @return A [Result] containing [ZhihuUser] on success.
     */
    suspend fun getGuestSelf(): Result<ZhihuUser> =
        api.safeApiCall {
            Request.Builder()
                .apiUrl("/guest/self")
                .get()
                .build()
        }

    /**
     * Retrieves the first page of the people an account follows.
     *
     * The client asks for the offset only; the page size is the server's own, and comes back in the
     * cursor it answers with.
     *
     * @param userId The account's user hash, not its url token.
     * @return A [Result] holding the page, or the failure that kept it away.
     */
    suspend fun followees(userId: String): Result<ZhihuFolloweesResponse> = api.safeApiCall {
        val url = "${BASE_URL}/people/$userId/followees".toHttpUrl().newBuilder()
            .addQueryParameter("offset", "0")
            .build()

        Request.Builder()
            .url(url)
            .get()
            .build()
    }

    /**
     * Retrieves a subsequent page of the people an account follows.
     *
     * @param nextUrl Absolute URL from the previous response's `paging.next`.
     * @return A [Result] holding the page, or the failure that kept it away.
     */
    suspend fun followeesByUrl(nextUrl: String): Result<ZhihuFolloweesResponse> = api.safeApiCall {
        Request.Builder()
            .url(nextUrl.toHttpUrl())
            .get()
            .build()
    }

    /**
     * Follows a user.
     *
     * @param userId The user hash ID (ZhihuUser.id, not url_token).
     * @return A [Result] indicating success or failure.
     */
    suspend fun followUser(userId: String): Result<Unit> =
        api.safeExecute {
            Request.Builder()
                .apiUrl("/people/$userId/followers")
                .post(ByteArray(0).toRequestBody())
                .build()
        }

    /**
     * Unfollows a user.
     *
     * @param userId The target user hash ID (ZhihuUser.id, not url_token).
     * @param currentUserId Current logged-in user's hash ID.
     * @return A [Result] indicating success or failure.
     */
    suspend fun unfollowUser(
        userId: String,
        currentUserId: String,
    ): Result<Unit> =
        api.safeExecute {
            Request.Builder()
                .apiUrl("/people/$userId/followers/$currentUserId")
                .delete(ByteArray(0).toRequestBody())
                .build()
        }

    private companion object {
        /** The token that stands in for the signed-in user. */
        const val SELF_TOKEN = "self"
    }
}
