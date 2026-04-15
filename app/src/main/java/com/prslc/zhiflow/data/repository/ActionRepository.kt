package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ReadHistoryRequest
import com.prslc.zhiflow.data.service.ActionService

class ActionRepository(private val service: ActionService) {

    /**
     * Votes on a piece of content (Upvote/Downvote/Cancel).
     * * @param id The target ID (answer or article)
     * @param type ContentType (e.g., ARTICLE, ANSWER)
     * @param action "up" or "down"
     * @param isRevoke If true, uses DELETE to cancel the vote
     */
    suspend fun vote(
        id: String,
        type: ContentType,
        action: String,
        isRevoke: Boolean = false
    ): Result<Boolean> = runCatching {
        val method = if (isRevoke) "DELETE" else "POST"
        service.voteAction(id, type, action, method)
    }

    /**
     * Syncs the reading history of a content to the server.
     */
    suspend fun syncHistory(request: ReadHistoryRequest): Result<Boolean> = runCatching {
        service.addReadHistory(request)
    }
}