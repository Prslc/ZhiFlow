package com.prslc.zhiflow.ui.viewmodel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.exception.ApiException
import com.prslc.zhiflow.data.exception.toApiException
import com.prslc.zhiflow.data.model.FeedItem
import com.prslc.zhiflow.data.service.getRecommendFeed
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

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
            try {
                isRefreshing = true
                error = null
                val response = getRecommendFeed(limit = 10, nextUrl = null)

                if (response != null) {
                    feedItems.clear()
                    val items = response.data.filter { it.target != null }
                    feedItems.addAll(items)

                    nextPageUrl = response.paging.next
                }
            } catch (e: Exception) {
                error = e.toApiException()
                e.printStackTrace()
            } finally {
                isRefreshing = false
            }
        }
    }

    fun loadMore() {
        if (isNextLoading || nextPageUrl == null) return
        viewModelScope.launch {
            try {
                isNextLoading = true
                val response = getRecommendFeed(nextUrl = nextPageUrl)

                if (response != null) {
                    val items = response.data.filter { it.target != null }
                    feedItems.addAll(items)
                    nextPageUrl = response.paging.next
                }
            } catch (e: Exception) {
                error = e.toApiException()
            } finally {
                isNextLoading = false
            }
        }
    }
}