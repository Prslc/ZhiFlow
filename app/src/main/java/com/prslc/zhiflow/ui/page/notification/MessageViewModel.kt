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
import com.prslc.zhiflow.data.dto.NotificationCategoryDto
import com.prslc.zhiflow.data.dto.UnreadSummaryDto
import com.prslc.zhiflow.data.repository.NotificationRepository
import kotlinx.coroutines.launch

class MessageViewModel(
    private val repository: NotificationRepository,
) : ViewModel() {

    @Immutable
    data class MessageUiState(
        val categories: List<NotificationCategoryDto> = emptyList(),
        val unread: UnreadSummaryDto? = null,
        val isRefreshing: Boolean = false,
        val globalError: ApiException? = null,
    )

    var uiState by mutableStateOf(MessageUiState())
        private set

    val listState = LazyListState()

    /**
     * Loads the page when nothing has been loaded yet.
     *
     * App start calls this, because the count the tab's badge draws arrives with the page and the
     * badge is on screen before the tab ever is.
     */
    fun loadIfEmpty() {
        if (uiState.unread != null) return
        fetch(reportVisit = false)
    }

    /**
     * Loads the page again, replacing what is on screen.
     *
     * Called when the tab is opened and when the reader pulls the page down. It reports the visit
     * with it, which is how the server counts the tab being opened.
     */
    fun refresh() = fetch(reportVisit = true)

    private fun fetch(reportVisit: Boolean) {
        if (uiState.isRefreshing) return

        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true, globalError = null)

            if (reportVisit) repository.reportTabAccess().ignoreOutcome()

            repository.messagePage()
                .onSuccess { result ->
                    uiState = uiState.copy(
                        categories = result.categories,
                        unread = result.unread,
                        isRefreshing = false,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(globalError = error, isRefreshing = false)
                }
        }
    }
}
