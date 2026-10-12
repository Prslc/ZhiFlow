package com.prslc.zhiflow.data.model.notification

import com.prslc.zhiflow.data.model.Paging
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A page of one conversation, newest first. */
@Serializable
data class ZhihuMessagesResponse(
    val data: List<ZhihuMessage> = emptyList(),
    val paging: Paging = Paging(),
)

/** One message in a conversation. [contentType] 0 is plain text. */
@Serializable
data class ZhihuMessage(
    val id: String = "",
    val content: String = "",
    @SerialName("content_type") val contentType: Int = 0,
    @SerialName("created_time") val createdTime: Long = 0,
    val sender: ZhihuMessageUser = ZhihuMessageUser(),
    val receiver: ZhihuMessageUser = ZhihuMessageUser(),
    @SerialName("is_canceled") val isCanceled: Boolean = false,
)

/**
 * One end of a conversation: the person it is with, or the reader themselves. The same shape answers
 * for the header of a conversation page.
 */
@Serializable
data class ZhihuMessageUser(
    val id: String = "",
    @SerialName("url_token") val urlToken: String = "",
    val name: String = "",
    val headline: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("message_user_type") val messageUserType: String = "",
    @SerialName("is_forbidden") val isForbidden: Boolean = false,
)
