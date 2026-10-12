package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.ChatMessageDto
import com.prslc.zhiflow.data.dto.ChatPeerDto
import com.prslc.zhiflow.data.model.notification.ZhihuMessage
import com.prslc.zhiflow.data.model.notification.ZhihuMessageUser
import com.prslc.zhiflow.data.model.notification.ZhihuMessagesResponse
import com.prslc.zhiflow.data.remote.parser.htmlToAnnotatedString

/**
 * A page of one conversation, as the server holds it: the newest message first.
 *
 * The order is left alone here because it is the order the page is drawn in — laid out from the
 * bottom, the newest message is the one the reader is looking for.
 *
 * @param peerId The person the conversation is with. A message from anyone else is the reader's own,
 *   which is how a bubble is placed on a side.
 * @return The messages, newest first, with their wording read out of the html the server writes.
 */
internal fun ZhihuMessagesResponse.toMessages(peerId: String): List<ChatMessageDto> =
    data.map { it.toDto(peerId) }

/**
 * One message, on the side it came from.
 *
 * @param peerId The person the conversation is with, as [toMessages] takes it.
 * @return The message as a bubble is drawn from it.
 */
internal fun ZhihuMessage.toDto(peerId: String): ChatMessageDto = ChatMessageDto(
    id = id,
    text = htmlToAnnotatedString(content),
    isMine = sender.id != peerId,
    createdTime = createdTime,
)

/** @return The person a conversation page is headed with. */
internal fun ZhihuMessageUser.toPeer(): ChatPeerDto = ChatPeerDto(
    name = name,
    avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
    headline = headline,
    isForbidden = isForbidden,
)
