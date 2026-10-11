package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.core.exception.mapOffMain
import com.prslc.zhiflow.data.dto.ChatMessageDto
import com.prslc.zhiflow.data.dto.ChatPeerDto
import com.prslc.zhiflow.data.mapper.toDto
import com.prslc.zhiflow.data.mapper.toMessages
import com.prslc.zhiflow.data.mapper.toPeer
import com.prslc.zhiflow.data.remote.service.MessageService

/** One page of a conversation, with the cursor for the page of older messages after it. */
data class ConversationResult(
    val messages: List<ChatMessageDto>,
    val nextPageUrl: String?,
    val isEnd: Boolean,
)

class MessageRepository(private val service: MessageService) {

    /**
     * Retrieves one page of a conversation.
     *
     * @param peerId The person the conversation is with, by their user hash.
     * @param nextUrl The page to ask for, from a previous result's cursor; null asks for the
     *   newest page.
     * @return A [Result] holding the messages in reading order, oldest first, and the cursor for
     *   the page before them when there is one.
     */
    suspend fun conversation(
        peerId: String,
        nextUrl: String? = null,
    ): Result<ConversationResult> = service.conversation(peerId, nextUrl)
        .mapOffMain { body ->
            ConversationResult(
                messages = body.toMessages(peerId),
                nextPageUrl = body.paging.next.takeIf { it.isNotBlank() },
                isEnd = body.paging.isEnd,
            )
        }

    /**
     * Sends one message into a conversation.
     *
     * @param peerId The person the conversation is with, by their user hash.
     * @param content The words to send.
     * @param sourceType How the page was opened, as its card said; null when it said nothing.
     * @return A [Result] holding the message as the server stored it.
     */
    suspend fun send(
        peerId: String,
        content: String,
        sourceType: String?,
    ): Result<ChatMessageDto> = service.send(peerId, content, sourceType)
        .mapOffMain { it.toDto(peerId) }

    /**
     * Deletes one message from this side of a conversation.
     *
     * @param messageId The message's own id.
     * @return A [Result] carrying the failure when the server refused.
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> = service.deleteMessage(messageId)

    /**
     * Retrieves who a conversation is with.
     *
     * @param peerId The same hash [conversation] takes.
     * @return A [Result] holding the person the page is headed by.
     */
    suspend fun peer(peerId: String): Result<ChatPeerDto> = service.peer(peerId).mapOffMain { it.toPeer() }
}
