package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.dto.BlockOptions
import com.prslc.zhiflow.data.dto.BlockTag
import com.prslc.zhiflow.data.dto.FeedbackAction
import com.prslc.zhiflow.data.mapper.toDto
import com.prslc.zhiflow.data.model.feedback.AggFeedbackRequest
import com.prslc.zhiflow.data.model.feedback.AggFeedbackTag
import com.prslc.zhiflow.data.model.feedback.ContentTagList
import com.prslc.zhiflow.data.model.feedback.DELIVER_NORMAL
import com.prslc.zhiflow.data.model.feedback.SCENE_RECOMMEND
import com.prslc.zhiflow.data.remote.service.FeedbackService

class FeedbackRepository(private val service: FeedbackService) {

    /**
     * Fetches the negative feedback panel for a piece of content.
     *
     * @param contentId The content token carried by the feed card.
     * @param contentType Lowercase as the feed spells it (`answer`, `article`, …).
     * @return A [Result] containing the rows to render, in the order the backend sent them.
     */
    suspend fun getPanel(contentId: String, contentType: String): Result<List<FeedbackAction>> {
        return service.getPanel(contentId, contentType)
            .map { panel -> panel.data.items.mapNotNull { it.toDto() } }
    }

    /**
     * Runs a panel row whose action is a request.
     *
     * @param url The `backend_url` that came with the row.
     * @param method The HTTP method that came with the same row.
     * @return A [Result] indicating success or failure.
     */
    suspend fun submit(url: String, method: String): Result<Unit> =
        service.submit(url, method)

    /**
     * Fetches both halves of the block page's state.
     *
     * @param contentToken The content token carried by the feed card.
     * @param contentType The numeric content type from the panel's `block_list` link.
     * @param feedbackType The feedback type from the same link.
     * @return A [Result] containing the topics on offer and the keywords already blocked.
     */
    suspend fun getBlockOptions(
        contentToken: String,
        contentType: String,
        feedbackType: String,
    ): Result<BlockOptions> {
        val tags: ContentTagList = service.getContentTags(contentToken, contentType, feedbackType)
            .getOrElse { return Result.failure(it) }

        return service.getBlockKeywords(contentToken, contentType, feedbackType)
            .map { block -> block.toDto(tags.data.map { tag -> tag.toDto() }) }
    }

    /**
     * Sends the block page's selection.
     *
     * @param tags Every topic the page offered, with the ones to block marked selected.
     * @param keywords The complete keyword list to send, not an increment.
     * @return A [Result] indicating success or failure.
     */
    suspend fun submitBlockFeedback(
        contentToken: String,
        contentType: String,
        tags: List<BlockTag>,
        keywords: List<String>,
    ): Result<Unit> = service.submitBlockFeedback(
        AggFeedbackRequest(
            contentType = contentType,
            contentToken = contentToken,
            sceneCode = SCENE_RECOMMEND,
            feedDeliverType = DELIVER_NORMAL,
            contentTags = tags.filter { it.isSelected }
                .map { AggFeedbackTag(id = it.id, value = it.label, isSelected = true) },
            keywords = keywords,
        )
    )
}
