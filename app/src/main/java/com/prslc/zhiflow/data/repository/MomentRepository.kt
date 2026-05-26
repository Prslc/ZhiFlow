package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.MomentsResponse
import com.prslc.zhiflow.data.service.MomentService

class MomentRepository(private val service: MomentService) {

    /**
     * Fetch the origin content of a moment by user token.
     *
     * @param urlToken The user's unique URL token.
     * @param nextUrl URL for the next page; if null, fetch the first page.
     * @return A [Result] containing [MomentsResponse] on success, or an exception on failure.
     */
    suspend fun getMoment(urlToken: String, nextUrl: String? = null): Result<MomentsResponse> =
        service.getMoment(urlToken, nextUrl)
}

