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
        return try {
            val response = service.getRecommendFeed(isRefresh, nextUrl)
            if (response != null) {
                val filteredData = response.data.filter { it.target != null }
                Result.success(response.copy(data = filteredData))
            } else {
                Result.failure(Exception("Empty or invalid response from server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}