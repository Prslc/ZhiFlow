package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.CollectionResponse
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import io.ktor.http.isSuccess

/**
 * Retrieve the list of collections (favorites) for a specific content item
 * @param id Content ID
 * @param contentType "answer" or "article"
 */
suspend fun getCollectionsForContent(id: String, contentType: String): CollectionResponse? {
    val typePath = if (contentType.lowercase().contains("article")) "article" else "answer"

    return try {
        val response = Client.client.get("collections/contents/$typePath/$id") {
            parameter("ever_top", 1)
        }

        if (response.status.isSuccess()) {
            response.body<CollectionResponse>()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Update the collection (favorite) status of a content item (add/remove from collections)
 * @param id Content ID
 * @param contentType "answer" or "article"
 */
suspend fun updateContentCollections(
    id: String,
    contentType: String,
    addIds: List<Long>,
    removeIds: List<Long>
): Boolean {
    val typePath = if (contentType.lowercase().contains("article")) "article" else "answer"

    return try {
        val response = Client.client.put("v2/collections/contents/$typePath/$id") {
            setBody(FormDataContent(Parameters.build {
                if (addIds.isNotEmpty()) {
                    append("add_collections", addIds.joinToString(","))
                }
                if (removeIds.isNotEmpty()) {
                    append("remove_collections", removeIds.joinToString(","))
                }
            }))
        }
        response.status.isSuccess()
    } catch (e: Exception) {
        false
    }
}