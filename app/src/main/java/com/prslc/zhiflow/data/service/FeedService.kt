package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.data.model.ZhihuResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class FeedService(private val client: HttpClient) {
    suspend fun getRecommendFeed(
        isRefresh: Boolean = false,
        nextUrl: String? = null
    ): ZhihuResponse? {
        return try {
            val requestUrl = nextUrl.takeUnless { isRefresh || nextUrl == null } ?: "topstory/recommend"
            val response = client.get(requestUrl) {
                if (isRefresh || nextUrl == null) {
                    parameter("action", "down")
                    parameter("start_type", "cold")
                    parameter("limit", 10)
                    parameter("is_feed_first_request", "1")
                    parameter("refresh_scene", "1")
                }
            }
            response.body<ZhihuResponse>()
        } catch (e: Exception) {
            null
        }
    }
}
