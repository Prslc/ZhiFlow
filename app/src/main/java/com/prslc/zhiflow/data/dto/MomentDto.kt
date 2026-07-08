package com.prslc.zhiflow.data.dto

import com.prslc.zhiflow.data.model.moment.MediaImage
import com.prslc.zhiflow.ui.page.people.moment.MomentContentType

data class MomentDto(
    val id: String,
    val type: MomentContentType,
    val title: String,
    val plainContent: String,
    val summary: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val routerUrl: String?,
    val voteCount: Int,
    val commentCount: Int,
    val collectCount: Int,
    val mediaImages: List<MediaImage>,
    val videoThumbnail: String?,
    val publishedAt: Long,
    val actionText: String,
    val actionTime: Long,
    val isTopping: Boolean,
)
