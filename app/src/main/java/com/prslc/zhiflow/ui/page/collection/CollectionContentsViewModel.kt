package com.prslc.zhiflow.ui.page.collection

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.data.dto.CollectionItemDto
import com.prslc.zhiflow.data.repository.CollectionRepository
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class CollectionContentsViewModel(
    private val repository: CollectionRepository,
) : ViewModel() {

    @Immutable
    data class CollectionContentsUiState(
        val items: List<CollectionItemDto> = emptyList(),
        val isRefreshing: Boolean = false,
        val isNextLoading: Boolean = false,
        val globalError: ApiException? = null,
        val loadMoreError: ApiException? = null,
    )

    var uiState by mutableStateOf(CollectionContentsUiState())
        private set

    val listState = LazyListState()
    private var currentUid: String? = null
    private var nextPageUrl: String? = null

    fun loadIfEmpty(uid: String) {
        if (uiState.items.isEmpty() || currentUid != uid) {
            currentUid = uid
            refresh()
        }
    }

    fun refresh() {
        val uid = currentUid ?: return
        if (uiState.isRefreshing) return

        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true, globalError = null)

            repository.getCollectionContents(uid, nextUrl = null)
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    uiState = uiState.copy(
                        items = result.items,
                        isRefreshing = false,
                        loadMoreError = null,
                    )
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    uiState = uiState.copy(globalError = e as? ApiException, isRefreshing = false)
                }
        }
    }

    fun loadMore() {
        if (uiState.isNextLoading || uiState.isRefreshing || nextPageUrl == null) return

        viewModelScope.launch {
            uiState = uiState.copy(isNextLoading = true, loadMoreError = null)

            repository.getCollectionContents(currentUid!!, nextUrl = nextPageUrl)
                .onSuccess { result ->
                    nextPageUrl = result.nextPageUrl
                    uiState = uiState.copy(
                        items = uiState.items + result.items,
                        isNextLoading = false,
                    )
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    uiState =
                        uiState.copy(loadMoreError = e as? ApiException, isNextLoading = false)
                }
        }
    }
}
