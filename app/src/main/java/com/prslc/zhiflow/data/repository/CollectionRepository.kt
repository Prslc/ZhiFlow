package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.user.CollectionResponse
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.remote.service.CollectionService
import com.prslc.zhiflow.data.dto.CollectionItemDto
import com.prslc.zhiflow.data.mapper.toDto
import com.prslc.zhiflow.data.session.UserSession

/**
 * Domain result for paginated collection contents.
 */
data class CollectionContentsResult(
    val items: List<CollectionItemDto>,
    val nextPageUrl: String?,
)

class CollectionRepository(
    private val service: CollectionService,
    private val session: UserSession,
) {

    /**
     * Retrieve the list of collections (favorites) for a specific content item.
     *
     * @param id Content ID
     * @param type Content type ([ContentType.ANSWER] or [ContentType.ARTICLE])
     */
    suspend fun getCollections(id: String, type: ContentType): Result<CollectionResponse> =
        service.getCollectionsForContent(id, type)

    /**
     * Retrieve a page of contents (answers) saved in the current user's collections.
     *
     * @param nextUrl Pagination URL from the previous response; null for the first page.
     */
    suspend fun getCollectionContents(
        nextUrl: String? = null,
    ): Result<CollectionContentsResult> {
        val response = if (nextUrl != null) {
            service.getCollectionContentsByUrl(nextUrl)
        } else {
            val uid = session.currentUserId()
                .getOrElse { return Result.failure(it) }
            service.getCollectionContents(uid)
        }

        return response
            .map { body ->
                val items = body.data
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
                    nextPageUrl = body.paging?.next,
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
    ): Result<Unit> {
        return service.updateContentCollections(
            id = id,
            contentType = type,
            addIds = add,
            removeIds = remove,
        )
    }
}
