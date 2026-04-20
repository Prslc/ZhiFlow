package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.core.network.body
import com.prslc.zhiflow.data.model.CollectionResponse
import com.prslc.zhiflow.data.model.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
     * @return A [CollectionResponse] containing collection details or null on failure.
     */
    suspend fun getCollectionsForContent(id: String, contentType: ContentType): CollectionResponse? =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.zhihu.com/collections/contents/${contentType.type}/$id"
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("ever_top", "1")
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().body<CollectionResponse>()
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Updates the collections for a specific content item by adding or removing it from folders.
     *
     * @param id The ID of the content.
     * @param contentType The [ContentType] of the target item.
     * @param addIds List of collection IDs to add the content to.
     * @param removeIds List of collection IDs to remove the content from.
     * @return True if the update was successful.
     */
    suspend fun updateContentCollections(
        id: String,
        contentType: ContentType,
        addIds: List<Long>,
        removeIds: List<Long>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Build the FormBody equivalent to Ktor's FormDataContent
            val formBuilder = FormBody.Builder()

            if (addIds.isNotEmpty()) {
                formBuilder.add("add_collections", addIds.joinToString(","))
            }
            if (removeIds.isNotEmpty()) {
                formBuilder.add("remove_collections", removeIds.joinToString(","))
            }

            val request = Request.Builder()
                .url("https://api.zhihu.com/v2/collections/contents/${contentType.type}/$id")
                .put(formBuilder.build())
                .build()

            okHttpClient.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}