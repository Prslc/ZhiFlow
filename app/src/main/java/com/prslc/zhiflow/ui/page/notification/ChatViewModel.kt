package com.prslc.zhiflow.ui.page.notification

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.ignoreOutcome
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.data.dto.ChatMessageDto
import com.prslc.zhiflow.data.dto.ChatPeerDto
import com.prslc.zhiflow.data.repository.MessageRepository
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: MessageRepository,
) : ViewModel() {

    @Immutable
    data class ChatUiState(
        val messages: List<ChatMessageDto> = emptyList(),
        val peer: ChatPeerDto? = null,
        val isLoading: Boolean = false,
        val isSending: Boolean = false,
        val error: ApiException? = null,
    )

    var uiState by mutableStateOf(ChatUiState())
        private set

    /** What is in the box right now, whether or not it has been sent. */
    var draft by mutableStateOf("")
        private set

    /** A send of the reader's that failed, which the page shows over the conversation. */
    var sendError by mutableStateOf<ApiException?>(null)
        private set

    /** @param value The box's new contents. */
    fun onDraftChange(value: String) {
        draft = value
    }

    /**
     * Loads one conversation, and who it is with.
     *
     * @param peerId The user hash of the person at the other end.
     */
    fun load(peerId: String) {
        if (uiState.isLoading) return

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)

            // The name and the picture are not worth failing the conversation over: without them
            // the page is headed by nothing, and the messages still read.
            var peer: ChatPeerDto? = null
            repository.peer(peerId)
                .onSuccess { peer = it }
                .ignoreOutcome()

            repository.conversation(peerId)
                .onSuccess { messages ->
                    uiState = ChatUiState(messages = messages, peer = peer)
                }
                .onApiFailure { error ->
                    uiState = ChatUiState(peer = peer, error = error)
                }
        }
    }

    /**
     * Sends what is in the box into the conversation, and puts the message the server hands back at
     * the end of the page.
     *
     * The box is emptied only once the server has taken the message: a send that fails leaves what
     * was typed where it is, and says so through [sendError] rather than costing the reader their
     * words.
     *
     * @param peerId The user hash of the person at the other end.
     * @param sourceType How the page was opened, as the card that opened it said.
     */
    fun send(peerId: String, sourceType: String?) {
        val content = draft.trim()
        if (content.isEmpty() || uiState.isSending) return

        viewModelScope.launch {
            uiState = uiState.copy(isSending = true)

            repository.send(peerId, content, sourceType)
                .onSuccess { message ->
                    draft = ""
                    uiState = uiState.copy(
                        messages = listOf(message) + uiState.messages,
                        isSending = false,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(isSending = false)
                    sendError = error
                }
        }
    }

    /** Clears the send failure the page has shown. */
    fun consumeSendError() {
        sendError = null
    }
}
