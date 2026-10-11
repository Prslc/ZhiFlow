package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.core.exception.mapOffMain
import com.prslc.zhiflow.data.dto.ChatMessageDto
import com.prslc.zhiflow.data.dto.ChatPeerDto
import com.prslc.zhiflow.data.mapper.toMessages
import com.prslc.zhiflow.data.mapper.toPeer
import com.prslc.zhiflow.data.remote.service.MessageService

class MessageRepository(private val service: MessageService) {

    /**
     * Retrieves one conversation.
     *
     * @param peerId The person the conversation is with, by their user hash.
     * @return A [Result] holding the newest page of messages in reading order.
     */
    suspend fun conversation(peerId: String): Result<List<ChatMessageDto>> =
        service.conversation(peerId).mapOffMain { it.toMessages(peerId) }

    /**
     * Retrieves who a conversation is with.
     *
     * @param peerId The same hash [conversation] takes.
     * @return A [Result] holding the person the page is headed by.
     */
    suspend fun peer(peerId: String): Result<ChatPeerDto> = service.peer(peerId).mapOffMain { it.toPeer() }
}
