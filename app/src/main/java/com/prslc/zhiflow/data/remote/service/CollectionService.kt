package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.Client
import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.data.model.user.CollectionResponse
import com.prslc.zhiflow.data.model.content.ContentType
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Service managing user collections (folders) and content categorization.
 */
class CollectionService(private val okHttpClient: OkHttpClient) {

    /**
     * Retrieves the list of collections associated with a specific piece of content.
     *
     * @param id The ID of the content.
     * @param contentType The [ContentType] of the target item.
     * @return A [Result] containing [CollectionResponse] on success.
     */
    suspend fun getCollectionsForContent(id: String, contentType: ContentType): Result<CollectionResponse> =
        okHttpClient.safeApiCall {
            val url = "${Client.BASE_URL}/collections/contents/${contentType.type}/$id"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("ever_top", "1")
                .build()

            Request.Builder()
                .url(url)
                .get()
                .build()
        }

    /**
     * Updates the collections for a specific content item by adding or removing it from folders.
     *
     * @param id The ID of the content.
     * @param contentType The [ContentType] of the target item.
     * @param addIds List of collection IDs to add the content to.
     * @param removeIds List of collection IDs to remove the content from.
     * @return A [Result] containing true if the update was successful.
     */
    suspend fun updateContentCollections(
        id: String,
        contentType: ContentType,
        addIds: List<Long>,
        removeIds: List<Long>
    ): Result<Boolean> = okHttpClient.safeExecute {
        val formBuilder = FormBody.Builder()

        if (addIds.isNotEmpty()) {
            formBuilder.add("add_collections", addIds.joinToString(","))
        }
        if (removeIds.isNotEmpty()) {
            formBuilder.add("remove_collections", removeIds.joinToString(","))
        }

        Request.Builder()
            .apiUrl("/v2/collections/contents/${contentType.type}/$id")
            .put(formBuilder.build())
            .build()
    }
}
