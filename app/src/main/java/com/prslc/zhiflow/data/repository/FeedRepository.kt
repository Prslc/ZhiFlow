package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.ZhihuResponse
import com.prslc.zhiflow.data.service.FeedService

class FeedRepository(private val service: FeedService) {

    /**
     * Fetch recommended feed from Zhihu (supports pagination and refresh)
     *
     * @param isRefresh If true, force refresh and fetch the first page
     * @param nextUrl URL for the next page; if null or isRefresh is true, fetch the first page
     * @return A [Result] containing [ZhihuResponse] on success, or an exception on failure
     */
    suspend fun getFeeds(isRefresh: Boolean, nextUrl: String?): Result<ZhihuResponse> {
        return service.getRecommendFeed(isRefresh, nextUrl)
            .map { response -> response.copy(data = response.data.filter { it.target != null }) }
    }
}