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

    suspend fun getPanel(contentId: String, contentType: String): Result<List<FeedbackAction>> {
        return service.getPanel(contentId, contentType)
            .map { panel -> panel.data.items.mapNotNull { it.toDto() } }
    }

    suspend fun submit(url: String, method: String): Result<Unit> =
        service.submit(url, method)

    /** Neither half of the block page is usable without the other, so one call fetches both. */
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
