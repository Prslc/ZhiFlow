package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.data.model.user.ReadHistoryRequest
import com.prslc.zhiflow.data.remote.service.ActionService

class ActionRepository(private val service: ActionService) {

    /**
     * Votes on a piece of content (Upvote/Downvote/Cancel).
     *
     * @param id The target ID (answer or article)
     * @param type ContentType (e.g., ARTICLE, ANSWER)
     * @param action "up" or "down"
     * @param isRevoke If true, uses DELETE to cancel the vote
     */
    suspend fun vote(
        id: String,
        type: ContentType,
        action: String,
        isRevoke: Boolean = false
    ): Result<Unit> {
        val method = if (isRevoke) "DELETE" else "POST"
        return service.voteAction(id, type, action, method)
    }

    /**
     * Likes the passage a `seg_like` range covers, or undoes that like.
     *
     * @param target The range. Undoing needs [SegmentLikeTarget.mySegId], so it must be the state
     *   the like request produced rather than the one the page was parsed with.
     * @return On like, the reader's own segment id to remember; undoing returns null.
     */
    suspend fun toggleSegmentLike(
        id: String,
        type: ContentType,
        target: SegmentLikeTarget,
        isLike: Boolean,
    ): Result<String?> {
        if (isLike) {
            return service.likeSegment(id, type, target).map { it.segId }
        }

        val mySegId = target.mySegId ?: return Result.success(null)
        return service.unlikeSegment(id, type, mySegId).map { null }
    }

    /**
     * Syncs the reading history of a content to the server.
     */
    suspend fun syncHistory(request: ReadHistoryRequest): Result<Unit> =
        service.addReadHistory(request)
}
