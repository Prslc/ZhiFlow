package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.dto.FeedDto
import com.prslc.zhiflow.data.mapper.toDto
import com.prslc.zhiflow.data.remote.service.FeedService

data class FeedResult(
    val items: List<FeedDto>,
    val nextPageUrl: String?,
)

class FeedRepository(private val service: FeedService) {

    /**
     * Fetch recommended feed from Zhihu (supports pagination and refresh)
     *
     * @param isRefresh If true, force refresh and fetch the first page
     * @param nextUrl URL for the next page; if null or isRefresh is true, fetch the first page
     * @return A [Result] containing [FeedResult] with mapped display items and next page URL
     */
    suspend fun getFeeds(isRefresh: Boolean, nextUrl: String?): Result<FeedResult> {
        return service.getRecommendFeed(isRefresh, nextUrl)
            .map { response ->
                FeedResult(
                    items = response.data.mapNotNull { it.toDto() },
                    nextPageUrl = response.paging.next,
                )
            }
    }
}
