package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.core.network.Client
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ReadHistoryRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class ActionService(private val client: HttpClient) {

    suspend fun addReadHistory(request: ReadHistoryRequest): Boolean {
        val response = client.post("read_history/add") {
            contentType(Json)
            setBody(request)
        }
        return response.status.isSuccess()
    }

    suspend fun voteAction(
        id: String,
        contentType: ContentType,
        action: String,
        method: String = "POST"
    ): Boolean {
        val response = client.request("reaction/${contentType.apiPath}/$id/vote/$action") {
            this.method = HttpMethod.parse(method)
        }
        return response.status.isSuccess()
    }
}