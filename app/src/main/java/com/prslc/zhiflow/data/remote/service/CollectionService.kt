package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.BASE_URL
import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.model.user.CollectionContentsResponse
import com.prslc.zhiflow.data.model.user.CollectionResponse
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
            val url = "${BASE_URL}/collections/contents/${contentType.type}/$id"
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
     * Retrieves paginated contents (answers) saved in a user's collections.
     *
     * When [nextUrl] is provided from a prior response's `paging.next`, it is used directly.
     * Otherwise, the initial request is constructed with [offset]=0 and [limit].
     *
     * @param uid The target user's ID.
     * @param nextUrl Absolute URL from the previous page; null for the first page.
     * @param limit Page size (default 20).
     */
    suspend fun getCollectionContents(
        uid: String,
        nextUrl: String? = null,
        limit: Int = 20
    ): Result<CollectionContentsResponse> = okHttpClient.safeApiCall {
        val baseUrl = nextUrl ?: "${BASE_URL}/people/$uid/collection_contents"
        val urlBuilder = baseUrl.toHttpUrl().newBuilder()

        if (nextUrl == null) {
            urlBuilder.addQueryParameter("offset", "0")
            urlBuilder.addQueryParameter("limit", limit.toString())
        }

        Request.Builder()
            .url(urlBuilder.build())
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
