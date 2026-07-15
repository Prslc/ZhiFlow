package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.user.CollectionResponse
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.remote.service.CollectionService
import com.prslc.zhiflow.data.dto.CollectionItemDto
import com.prslc.zhiflow.data.mapper.toDto

/**
 * Domain result for paginated collection contents.
 */
data class CollectionContentsResult(
    val items: List<CollectionItemDto>,
    val nextPageUrl: String?,
)

class CollectionRepository(private val service: CollectionService) {

    /**
     * Retrieve the list of collections (favorites) for a specific content item.
     *
     * @param id Content ID
     * @param type Content type ([ContentType.ANSWER] or [ContentType.ARTICLE])
     */
    suspend fun getCollections(id: String, type: ContentType): Result<CollectionResponse> =
        service.getCollectionsForContent(id, type)

    /**
     * Retrieve paginated contents of a user's collections.
     *
     * @param uid The target user's ID.
     * @param nextUrl Pagination URL from the previous response; null for the first page.
     */
    suspend fun getCollectionContents(
        uid: String,
        nextUrl: String? = null,
    ): Result<CollectionContentsResult> {
        return service.getCollectionContents(uid, nextUrl)
            .map { response ->
                val items = response.data
                    .map { it.toDto() }
                    .groupBy { it.id }
                    .values
                    .map { group ->
                        if (group.size == 1) {
                            group.first()
                        } else {
                            group.first().copy(
                                collectionNames = group.flatMap { it.collectionNames }.distinct(),
                            )
                        }
                    }
                CollectionContentsResult(
                    items = items,
                    nextPageUrl = response.paging?.next,
                )
            }
    }

    /**
     * Update the collection status (add/remove) of a content item.
     *
     * @param id Content ID
     * @param type Content type ([ContentType.ANSWER] or [ContentType.ARTICLE])
     * @param add List of collection IDs to add the content to
     * @param remove List of collection IDs to remove the content from
     */
    suspend fun updateCollections(
        id: String,
        type: ContentType,
        add: List<Long>,
        remove: List<Long>
    ): Result<Boolean> {
        return service.updateContentCollections(
            id = id,
            contentType = type,
            addIds = add,
            removeIds = remove,
        )
    }
}
