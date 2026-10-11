package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString

/**
 * One message of a conversation, ready to be drawn as a bubble.
 *
 * [text] carries the marks the server wrote into the message: its links are kept as tappable ranges
 * and its emoji as the images this app draws them with, which is why it is not a plain string.
 */
@Immutable
data class ChatMessageDto(
    val id: String,
    val text: AnnotatedString,
    val isMine: Boolean,
    val createdTime: Long,
)

/** The person a conversation is with, as the page's header draws them. */
@Immutable
data class ChatPeerDto(
    val name: String,
    val avatarUrl: String?,
    val headline: String,
    val isForbidden: Boolean,
)
