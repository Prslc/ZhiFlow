package com.prslc.zhiflow.ui.page.notification

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.ignoreOutcome
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.core.utils.compose.withPage
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
        val isDeleting: Boolean = false,
        val isSelecting: Boolean = false,
        val isNextLoading: Boolean = false,
        val isEnd: Boolean = false,
        val selectedIds: Set<String> = emptySet(),
        val globalError: ApiException? = null,
        val loadMoreError: ApiException? = null,
    )

    var uiState by mutableStateOf(ChatUiState())
        private set

    val listState = LazyListState()

    private var nextPageUrl: String? = null

    /** What is in the box right now, whether or not it has been sent. */
    var draft by mutableStateOf("")
        private set

    /** A write of the reader's that failed, which the page shows over the conversation. */
    var actionError by mutableStateOf<ApiException?>(null)
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
            uiState = uiState.copy(isLoading = true, globalError = null)

            // The name and the picture are not worth failing the conversation over: without them
            // the page is headed by nothing, and the messages still read.
            var peer: ChatPeerDto? = null
            repository.peer(peerId)
                .onSuccess { peer = it }
                .ignoreOutcome()

            repository.conversation(peerId)
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    uiState = ChatUiState(
                        messages = uiState.messages.withPage(result.messages, firstPage = true) {
                            it.id
                        },
                        peer = peer,
                        isEnd = result.isEnd,
                    )
                }
                .onApiFailure { error ->
                    uiState = ChatUiState(peer = peer, globalError = error)
                }
        }
    }

    /**
     * Loads the page of older messages before the ones on screen.
     *
     * They go on the end of the list, which is the top of the page: the conversation is held
     * newest first.
     *
     * @param peerId The user hash of the person at the other end.
     */
    fun loadMore(peerId: String) {
        val url = nextPageUrl ?: return
        if (uiState.isNextLoading || uiState.isLoading) return

        viewModelScope.launch {
            uiState = uiState.copy(isNextLoading = true, loadMoreError = null)

            repository.conversation(peerId, url)
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    uiState = uiState.copy(
                        messages = uiState.messages.withPage(result.messages, firstPage = false) {
                            it.id
                        },
                        isNextLoading = false,
                        isEnd = result.isEnd,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(loadMoreError = error, isNextLoading = false)
                }
        }
    }

    /**
     * Sends what is in the box into the conversation, and puts the message the server hands back at
     * the end of the page.
     *
     * The box is emptied only once the server has taken the message: a send that fails leaves what
     * was typed where it is, and says so through [actionError] rather than costing the reader their
     * words.
     *
     * The page is told the message is in, through [messageSent], and brings the reader to it: a
     * message that arrives while they are at the newest one does not come into view on its own.
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
                    messageSent = true
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(isSending = false)
                    actionError = error
                }
        }
    }

    /** The message the reader is holding down, whose actions the menu offers. */
    var menuMessage by mutableStateOf<ChatMessageDto?>(null)
        private set

    /** Set once a message has gone out, for the page to bring the reader to it. */
    var messageSent by mutableStateOf(false)
        private set

    /** Clears the send the page has brought the reader to. */
    fun consumeSent() {
        messageSent = false
    }

    /** @param message The message whose actions the menu is to offer. */
    fun openMenu(message: ChatMessageDto) {
        menuMessage = message
    }

    /** Closes the menu. */
    fun closeMenu() {
        menuMessage = null
    }

    /**
     * Deletes the message the menu is offering, and takes it off the page.
     *
     * The server keeps nothing in its place, so neither does the page: the row is dropped rather
     * than drawn as something that was once there. The menu closes either way, so that what went
     * wrong is not said behind it.
     */
    fun deleteMenuMessage() {
        val message = menuMessage ?: return
        if (uiState.isDeleting) return

        viewModelScope.launch {
            uiState = uiState.copy(isDeleting = true)

            repository.deleteMessage(message.id)
                .onSuccess {
                    menuMessage = null
                    uiState = uiState.copy(
                        messages = uiState.messages.filterNot { it.id == message.id },
                        isDeleting = false,
                    )
                }
                .onApiFailure { error ->
                    menuMessage = null
                    uiState = uiState.copy(isDeleting = false)
                    actionError = error
                }
        }
    }

    /** Clears the failure the page has shown. */
    fun consumeActionError() {
        actionError = null
    }

    /** Reads the page as a list of messages to pick from, which is the menu's own last entry. */
    fun startSelecting() {
        menuMessage = null
        uiState = uiState.copy(isSelecting = true, selectedIds = emptySet())
    }

    /** Leaves that reading, picking nothing. */
    fun cancelSelecting() {
        uiState = uiState.copy(isSelecting = false, selectedIds = emptySet())
    }

    /**
     * Adds one message to the picking, or takes it out again.
     *
     * @param messageId The message the reader tapped.
     */
    fun toggleSelected(messageId: String) {
        uiState = uiState.copy(
            selectedIds = if (messageId in uiState.selectedIds) {
                uiState.selectedIds - messageId
            } else {
                uiState.selectedIds + messageId
            },
        )
    }

    /**
     * Deletes every message picked out, and takes them off the page.
     *
     * They go one at a time, since the server deletes one at a time; a refusal is reported like any
     * other write and does not hold up the rest. The picking ends either way, so that the reader is
     * not left standing in it with nothing to do next.
     */
    fun deleteSelected() {
        val ids = uiState.selectedIds
        if (ids.isEmpty() || uiState.isDeleting) return

        viewModelScope.launch {
            uiState = uiState.copy(isDeleting = true)

            var failure: ApiException? = null
            val deleted = mutableSetOf<String>()

            ids.forEach { id ->
                repository.deleteMessage(id)
                    .onSuccess { deleted += id }
                    .onApiFailure { error -> if (failure == null) failure = error }
            }

            uiState = uiState.copy(
                messages = uiState.messages.filterNot { it.id in deleted },
                isSelecting = false,
                selectedIds = emptySet(),
                isDeleting = false,
            )
            actionError = failure
        }
    }
}
