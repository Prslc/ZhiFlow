package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.data.model.ZhihuAnswer
import com.prslc.zhiflow.data.model.ZhihuArticle
import com.prslc.zhiflow.data.model.ZhihuContent
import com.prslc.zhiflow.data.model.ZhihuPin
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess

class ContentService(private val client: HttpClient) {

    private suspend inline fun <reified T : ZhihuContent> fetchContent(
        path: String,
        id: String
    ): T? {
        return try {
            val response = client.get("$path/v2/$id")
            if (response.status.isSuccess()) response.body<T>() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAnswerDetail(id: String) = fetchContent<ZhihuAnswer>("answers", id)
    suspend fun getArticleDetail(id: String) = fetchContent<ZhihuArticle>("articles", id)
    suspend fun getPinDetail(id: String) = fetchContent<ZhihuPin>("pins", id)
}