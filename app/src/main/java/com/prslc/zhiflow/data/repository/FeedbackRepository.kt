package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.dto.FeedbackAction
import com.prslc.zhiflow.data.mapper.toDto
import com.prslc.zhiflow.data.remote.service.FeedbackService

class FeedbackRepository(private val service: FeedbackService) {

    suspend fun getPanel(contentId: String, contentType: String): Result<List<FeedbackAction>> {
        return service.getPanel(contentId, contentType)
            .map { panel -> panel.data.items.mapNotNull { it.toDto() } }
    }

    suspend fun submit(url: String, method: String): Result<Unit> =
        service.submit(url, method)
}
