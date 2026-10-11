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
        val error: ApiException? = null,
    )

    var uiState by mutableStateOf(ChatUiState())
        private set

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
}
