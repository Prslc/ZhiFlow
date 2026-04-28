package com.prslc.zhiflow.ui.page.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.toApiException
import com.prslc.zhiflow.data.model.FeedItem
import com.prslc.zhiflow.data.repository.FeedRepository
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class FeedViewModel(private val repository: FeedRepository) : ViewModel() {

    data class FeedUiState(
        val items: List<FeedItem> = emptyList(),
        val isRefreshing: Boolean = false,
        val isNextLoading: Boolean = false,
        val error: ApiException? = null
    )

    var uiState by mutableStateOf(FeedUiState())
        private set

    val listState = LazyListState()
    private var nextPageUrl: String? = null

    fun loadIfEmpty() {
        if (uiState.items.isEmpty()) {
            refresh()
        }
    }

    fun refresh() {
        if (uiState.isRefreshing) return
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true, error = null)

            repository.getFeeds(isRefresh = true, nextUrl = null)
                .onSuccess { response ->
                    nextPageUrl = response.paging.next
                    uiState = uiState.copy(items = response.data, isRefreshing = false)
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    uiState = uiState.copy(error = e.toApiException(), isRefreshing = false)
                }
        }
    }

    fun loadMore() {
        if (uiState.isNextLoading || nextPageUrl == null) return

        viewModelScope.launch {
            uiState = uiState.copy(isNextLoading = true)

            repository.getFeeds(isRefresh = false, nextUrl = nextPageUrl)
                .onSuccess { response ->
                    nextPageUrl = response.paging.next
                    uiState = uiState.copy(
                        items = uiState.items + response.data,
                        isNextLoading = false
                    )
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    uiState = uiState.copy(error = e.toApiException(), isNextLoading = false)
                }
        }
    }
}
