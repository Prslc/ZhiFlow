package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.ui.component.common.ImageData

@Immutable
data class AnswerDto(
    val id: String,
    val authorName: String,
    val authorAvatar: String?,
    val excerpt: String,
    val images: List<ImageData>,
    val voteCount: Int,
    val commentCount: Int,
)
