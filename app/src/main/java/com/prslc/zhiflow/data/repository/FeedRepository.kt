package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.dto.FeedDto
import com.prslc.zhiflow.data.mapper.toDto
import com.prslc.zhiflow.data.remote.service.FeedService

data class FeedResult(
    val items: List<FeedDto>,
    val nextPageUrl: String?,
    val previousPageUrl: String?,
    val isEnd: Boolean,
)

class FeedRepository(private val service: FeedService) {

    /**
     * Fetch recommended feed from Zhihu (supports pagination and refresh)
     *
     * @param isColdStart True for the session's first page, false for a pull-to-refresh and for
     *   paging; it is what tells the server this is the head of a new session.
     * @param nextUrl The page to request, taken from a previous response: `paging.previous` when
     *   refreshing, `paging.next` when paging. Null asks for the first page.
     * @return A [Result] containing [FeedResult] with mapped display items, both page URLs, and
     *   whether the server has said the feed ends here.
     */
    suspend fun getFeeds(isColdStart: Boolean, nextUrl: String?): Result<FeedResult> {
        return service.getRecommendFeed(isColdStart, nextUrl)
            .map { response ->
                FeedResult(
                    items = response.data.mapNotNull { it.toDto() },
                    nextPageUrl = response.paging.next,
                    previousPageUrl = response.paging.previous,
                    isEnd = response.paging.isEnd,
                )
            }
    }
}
