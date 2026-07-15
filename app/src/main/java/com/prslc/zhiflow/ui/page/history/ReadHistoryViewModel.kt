package com.prslc.zhiflow.ui.page.history

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.data.dto.ReadHistoryDto
import com.prslc.zhiflow.data.repository.ReadHistoryRepository
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

sealed interface HistoryListItem {
    /** Question-level header displayed before its grouped answer items. */
    data class Header(
        val questionTitle: String,
        val questionToken: String,
        val icon: String?,
    ) : HistoryListItem

    /** Individual read-history entry (answer to a question). */
    data class Entry(val item: ReadHistoryDto, val isInGroup: Boolean = false) : HistoryListItem
}

class ReadHistoryViewModel(
    private val repository: ReadHistoryRepository
) : ViewModel() {

    @Immutable
    data class HistoryUiState(
        val items: List<HistoryListItem> = emptyList(),
        val isRefreshing: Boolean = false,
        val isNextLoading: Boolean = false,
        val isEnd: Boolean = false,
        val globalError: ApiException? = null,
        val loadMoreError: ApiException? = null,
    )

    var uiState by mutableStateOf(HistoryUiState())
        private set

    val listState = LazyListState()
    private var nextOffset = 0

    fun loadIfEmpty() {
        if (uiState.items.isEmpty()) refresh()
    }

    fun refresh() {
        if (uiState.isRefreshing) return
        viewModelScope.launch {
            uiState = uiState.copy(isRefreshing = true, globalError = null)

            repository.getReadHistory(offset = 0)
                .onSuccess { result ->
                    nextOffset = PAGE_SIZE
                    uiState = uiState.copy(
                        items = buildDisplayList(result.items),
                        isRefreshing = false,
                        isEnd = result.isEnd,
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
        if (uiState.isNextLoading || uiState.isRefreshing || uiState.isEnd) return
        viewModelScope.launch {
            uiState = uiState.copy(isNextLoading = true, loadMoreError = null)

            repository.getReadHistory(offset = nextOffset)
                .onSuccess { result ->
                    nextOffset += PAGE_SIZE
                    uiState = uiState.copy(
                        items = uiState.items + buildDisplayList(result.items),
                        isNextLoading = false,
                        isEnd = result.isEnd,
                    )
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    uiState = uiState.copy(loadMoreError = e as? ApiException, isNextLoading = false)
                }
        }
    }

    companion object {
        private const val PAGE_SIZE = 10
    }

    /**
     * Groups flat [ReadHistoryDto] items by contiguous [ReadHistoryDto.questionToken]
     * segments. A [HistoryListItem.Header] is inserted only when a contiguous segment
     * contains 2+ groupable entries. Non-groupable types (question, profile) appear
     * standalone and break any ongoing group.
     */
    private fun buildDisplayList(items: List<ReadHistoryDto>): List<HistoryListItem> {
        val result = mutableListOf<HistoryListItem>()
        var i = 0
        while (i < items.size) {
            val item = items[i]
            val key = item.questionToken.orEmpty()
            val isGroupable = key.isNotEmpty() && item.contentType !in listOf("question", "profile")

            if (isGroupable) {
                // Collect contiguous items sharing the same question token
                val segment = mutableListOf<ReadHistoryDto>()
                while (i < items.size) {
                    val cur = items[i]
                    val curKey = cur.questionToken.orEmpty()
                    if (curKey == key && cur.contentType !in listOf("question", "profile")) {
                        segment.add(cur)
                        i++
                    } else {
                        break
                    }
                }
                val inGroup = segment.size > 1
                if (inGroup) {
                    result.add(
                        HistoryListItem.Header(
                            questionTitle = segment.first().questionTitle,
                            questionToken = key,
                            icon = segment.first().contentTypeIcon,
                        )
                    )
                }
                segment.forEach { result.add(HistoryListItem.Entry(it, isInGroup = inGroup)) }
            } else {
                result.add(HistoryListItem.Entry(item))
                i++
            }
        }
        return result
    }
}
