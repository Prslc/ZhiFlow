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
import kotlinx.coroutines.launch

class FeedViewModel(private val repository: FeedRepository) : ViewModel() {

    var feedItems = mutableStateListOf<FeedItem>()

    var error by mutableStateOf<ApiException?>(null)

    val listState = LazyListState()
    var isRefreshing by mutableStateOf(false)
    var isNextLoading by mutableStateOf(false)
    private var nextPageUrl: String? = null

    fun loadIfEmpty() {
        if (feedItems.isEmpty()) {
            refresh()
        }
    }

    fun refresh() {
        if (isRefreshing) return
        viewModelScope.launch {
            isRefreshing = true
            error = null

            repository.fetchFeeds(isRefresh = true, nextUrl = null)
                .onSuccess { response ->
                    feedItems.clear()
                    feedItems.addAll(response.data)
                    nextPageUrl = response.paging.next
                }
                .onFailure { error = it.toApiException() }

            isRefreshing = false
        }
    }

    fun loadMore() {
        if (isNextLoading || nextPageUrl == null) return
        viewModelScope.launch {
            isNextLoading = true
            repository.fetchFeeds(isRefresh = false, nextUrl = nextPageUrl)
                .onSuccess { response ->
                    feedItems.addAll(response.data)
                    nextPageUrl = response.paging.next
                }
                .onFailure { error = it.toApiException() }
            isNextLoading = false
        }
    }
}