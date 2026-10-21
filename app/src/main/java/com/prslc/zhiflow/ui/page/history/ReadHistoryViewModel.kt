package com.prslc.zhiflow.ui.page.history

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.core.utils.compose.withPage
import com.prslc.zhiflow.data.dto.ReadHistoryDto
import com.prslc.zhiflow.data.repository.ReadHistoryRepository
import kotlinx.coroutines.launch

sealed interface HistoryListItem {
    /** The identity this row is drawn under; a page's rows are unique by it. */
    val key: String

    /** Question-level header displayed before its grouped answer items. */
    data class Header(
        val questionTitle: String,
        val questionToken: String,
        val icon: String?,
        override val key: String,
    ) : HistoryListItem

    /** Individual read-history entry (answer to a question). */
    data class Entry(
        val item: ReadHistoryDto,
        val isInGroup: Boolean = false,
    ) : HistoryListItem {
        override val key: String get() = "e_${item.identity}"
    }
}

/**
 * What names one history row: the content it points at, or — for a row that points at nothing,
 * such as a profile — the moment it was read.
 */
private val ReadHistoryDto.identity: String
    get() = contentToken?.takeIf { it.isNotBlank() } ?: "q_${questionToken.orEmpty()}-$readTime"

class ReadHistoryViewModel(
    private val repository: ReadHistoryRepository,
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
                        items = buildDisplayList(result.items).distinctBy { it.key },
                        isRefreshing = false,
                        isEnd = result.isEnd,
                        loadMoreError = null,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(globalError = error, isRefreshing = false)
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
                        items = uiState.items.withPage(
                            buildDisplayList(result.items),
                            firstPage = false,
                        ) { it.key },
                        isNextLoading = false,
                        isEnd = result.isEnd,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(loadMoreError = error, isNextLoading = false)
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
                            // Two stretches of the same question are two headers, so the question
                            // alone cannot name one: the entry the stretch opens with does.
                            key = "h_${segment.first().identity}",
                        ),
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
