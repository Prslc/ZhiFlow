package com.prslc.zhiflow.ui.page.feed

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.toApiException
import com.prslc.zhiflow.data.model.FeedItem
import com.prslc.zhiflow.data.repository.FeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedViewModel(private val repository: FeedRepository) : ViewModel() {

    data class FeedUiState(
        val items: List<FeedItem> = emptyList(),
        val isRefreshing: Boolean = false,
        val isNextLoading: Boolean = false,
        val error: ApiException? = null
    )

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState = _uiState.asStateFlow()

    val listState = LazyListState()
    private var nextPageUrl: String? = null

    fun loadIfEmpty() {
        if (uiState.value.items.isEmpty()) {
            refresh()
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            repository.fetchFeeds(isRefresh = true, nextUrl = null)
                .onSuccess { response ->
                    nextPageUrl = response.paging.next
                    _uiState.update {
                        it.copy(items = response.data, isRefreshing = false)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(error = e.toApiException(), isRefreshing = false)
                    }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isNextLoading || nextPageUrl == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isNextLoading = true) }

            repository.fetchFeeds(isRefresh = false, nextUrl = nextPageUrl)
                .onSuccess { response ->
                    nextPageUrl = response.paging.next
                    _uiState.update {
                        it.copy(items = it.items + response.data, isNextLoading = false)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(error = e.toApiException(), isNextLoading = false)
                    }
                }
        }
    }
}