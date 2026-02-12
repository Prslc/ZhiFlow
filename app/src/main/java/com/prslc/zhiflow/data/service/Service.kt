package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.ZhihuResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

suspend fun getRecommendFeed(limit: Int = 10, nextUrl: String? = null): ZhihuResponse? {
    val TAG = "ZhihuNetwork"
    return try {
        // 如果有 nextUrl，说明是在翻页，直接请求知乎给的完整链接
        // 如果没有，则是刷新，请求初始推荐路径
        val requestUrl = nextUrl ?: "feed/topstory/recommend"

        val response = Client.client.get(requestUrl) {
            // 只有初始请求需要手动传 limit 参数
            if (nextUrl == null) {
                parameter("limit", limit)
                parameter("action", "down")
            }
        }

        Log.d(TAG, "Requesting: $requestUrl | Status: ${response.status}")

        // 直接返回整个 Response 对象
        response.body<ZhihuResponse>()
    } catch (e: Exception) {
        Log.e(TAG, "Network Error", e)
        null
    }
}