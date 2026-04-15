package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.data.model.CollectionResponse
import com.prslc.zhiflow.data.model.ContentType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import io.ktor.http.isSuccess

class CollectionService(private val client: HttpClient) {

    suspend fun getCollectionsForContent(id: String, contentType: ContentType): CollectionResponse? {
        return try {
            val response = client.get("collections/contents/${contentType.type}/$id") {
                parameter("ever_top", 1)
            }
            if (response.status.isSuccess()) response.body() else null
        } catch (e: Exception) { null }
    }

    suspend fun updateContentCollections(
        id: String,
        contentType: ContentType,
        addIds: List<Long>,
        removeIds: List<Long>
    ): Boolean {
        return try {
            val response = client.put("v2/collections/contents/${contentType.type}/$id") {
                setBody(FormDataContent(Parameters.build {
                    if (addIds.isNotEmpty()) append("add_collections", addIds.joinToString(","))
                    if (removeIds.isNotEmpty()) append("remove_collections", removeIds.joinToString(","))
                }))
            }
            response.status.isSuccess()
        } catch (e: Exception) { false }
    }
}