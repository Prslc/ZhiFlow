package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.CollectionResponse
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.service.CollectionService

class CollectionRepository(private val service: CollectionService) {

    /**
     * Retrieve the list of collections (favorites) for a specific content item
     * @param id Content ID
     * @param type Content type ([ContentType.ANSWER] or [ContentType.ARTICLE])
     */
    suspend fun getCollections(id: String, type: ContentType): Result<CollectionResponse> {
        val data = service.getCollectionsForContent(id, type)
        return if (data != null) Result.success(data)
        else Result.failure(Exception("Failed to retrieve favorites"))
    }

    /**
     * Update the collection status (add/remove) of a content item
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
    ): Boolean {
        return service.updateContentCollections(
            id = id,
            contentType = type,
            addIds = add,
            removeIds = remove
        )
    }
}