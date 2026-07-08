package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.moment.MomentsFeedResponse
import com.prslc.zhiflow.data.model.moment.MomentsResponse
import com.prslc.zhiflow.data.remote.service.MomentService

class MomentRepository(private val service: MomentService) {

    /**
     * Fetch the origin content of a moment by user token.
     *
     * @param urlToken The user's unique URL token.
     * @param nextUrl URL for the next page; if null, fetch the first page.
     * @return A [Result] containing [MomentsResponse] on success, or an exception on failure.
     */
    suspend fun getUserPost(urlToken: String, nextUrl: String? = null): Result<MomentsResponse> =
        service.getUserPosts(urlToken, nextUrl)

    /**
     * Fetch the user's activity stream (e.g. publication history and interactions).
     *
     * @param urlToken The user's unique URL token.
     * @param nextUrl URL for the next page; if null, fetch the first page.
     * @return A [Result] containing [MomentsFeedResponse] on success, or an exception on failure.
     */
    suspend fun getUserActivities(urlToken: String, nextUrl: String? = null): Result<MomentsFeedResponse> =
        service.getUserActivities(urlToken, nextUrl)

    /**
     * Fetch the upvoted content of a user.
     *
     * @param urlToken The user's unique URL token.
     * @param nextUrl URL for the next page; if null, fetch the first page.
     * @return A [Result] containing [MomentsFeedResponse] on success, or an exception on failure.
     */
    suspend fun getUserVote(urlToken: String, nextUrl: String? = null): Result<MomentsFeedResponse> =
        service.getUserVote(urlToken, nextUrl)
}
