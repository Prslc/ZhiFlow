package com.prslc.zhiflow.ui.page.notification

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.data.dto.NotificationItemDto
import com.prslc.zhiflow.data.repository.NotificationRepository
import kotlinx.coroutines.launch

class NotificationCategoryViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {

    @Immutable
    data class CategoryUiState(
        val items: List<NotificationItemDto> = emptyList(),
        val isLoading: Boolean = false,
        val isNextLoading: Boolean = false,
        val isMarkingRead: Boolean = false,
        val globalError: ApiException? = null,
        val loadMoreError: ApiException? = null,
    ) {
        /** Whether anything on screen still counts as unread, which is what the action is for. */
        val hasUnread: Boolean
            get() = items.any { it is NotificationItemDto.Entry && !it.isRead }
    }

    var uiState by mutableStateOf(CategoryUiState())
        private set

    /** A write of the reader's that failed, which the screen shows over its list. */
    var actionError by mutableStateOf<ApiException?>(null)
        private set

    val listState = LazyListState()

    private var type = ""
    private var nextPageUrl: String? = null

    /**
     * Loads the list the page is for, once.
     *
     * @param type Which list to read, in the spelling the message page names it by.
     */
    fun load(type: String) {
        if (this.type == type) return
        this.type = type
        fetch()
    }

    /** Loads the first page again, replacing what is on screen. */
    fun refresh() {
        if (type.isEmpty()) return
        fetch()
    }

    /** Loads the next page. No-op at the end, mid-load, or with none. */
    fun loadMore() {
        val url = nextPageUrl ?: return
        if (uiState.isNextLoading || uiState.isLoading) return

        viewModelScope.launch {
            uiState = uiState.copy(isNextLoading = true, loadMoreError = null)

            repository.notifications(type, url)
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    uiState = uiState.copy(
                        items = uiState.items + result.items,
                        isNextLoading = false,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(loadMoreError = error, isNextLoading = false)
                }
        }
    }

    /**
     * Marks every notification in this list as read, then loads it again so the rows stop reading
     * as unread.
     *
     * A failure goes to [actionError] rather than to the page: there is still a list on screen, and
     * it is still worth reading.
     */
    fun markAllRead() {
        if (uiState.isMarkingRead || type.isEmpty()) return

        viewModelScope.launch {
            uiState = uiState.copy(isMarkingRead = true)

            repository.markAllRead(type)
                .onSuccess {
                    uiState = uiState.copy(isMarkingRead = false)
                    fetch()
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(isMarkingRead = false)
                    actionError = error
                }
        }
    }

    /** Clears the write failure the screen has shown. */
    fun consumeActionError() {
        actionError = null
    }

    private fun fetch() {
        if (uiState.isLoading) return

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, globalError = null)

            repository.notifications(type)
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    uiState = uiState.copy(
                        items = result.items,
                        isLoading = false,
                        loadMoreError = null,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(globalError = error, isLoading = false)
                }
        }
    }
}
