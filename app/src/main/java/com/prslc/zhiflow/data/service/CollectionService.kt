package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.data.model.CollectionResponse
import com.prslc.zhiflow.data.api.Client
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import io.ktor.http.isSuccess

object CollectionService {

    /**
     * Fetch all collection folders for a specific answer
     */
    suspend fun getCollectionsForAnswer(answerId: String): CollectionResponse? {
        return try {
            val response = Client.client.get("collections/contents/answer/$answerId") {
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
     * Update answer collection status (Add/Remove from folders)
     */
    suspend fun updateAnswerCollections(
        answerId: String,
        addIds: List<Long>,
        removeIds: List<Long>
    ): Boolean {
        return try {
            val response = Client.client.put("v2/collections/contents/answer/$answerId") {
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
}