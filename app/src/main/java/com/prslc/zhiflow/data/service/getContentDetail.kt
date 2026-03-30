package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.ZhihuAnswer
import com.prslc.zhiflow.data.model.ZhihuArticle
import com.prslc.zhiflow.data.model.ZhihuContent
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Generic content retrieval function
 * @param path API endpoint path, e.g., "answers/v2" or "articles/v2"
 * @param id Content ID
 */
private suspend inline fun <reified T : ZhihuContent> fetchContent(
    path: String,
    id: String
): T? {
    val tag = "ContentService"
    return try {
        val response = Client.client.get("$path/$id")

        if (response.status.value != 200) {
            Log.e(tag, "Request failed with status: ${response.status}")
            return null
        }

        response.body<T>()
    } catch (e: Exception) {
        Log.e(tag, "Failed to fetch profile", e)
        throw e
    }
}

suspend fun getAnswerDetail(id: String) = fetchContent<ZhihuAnswer>("answers/v2", id)
suspend fun getArticleDetail(id: String) = fetchContent<ZhihuArticle>("articles/v2", id)