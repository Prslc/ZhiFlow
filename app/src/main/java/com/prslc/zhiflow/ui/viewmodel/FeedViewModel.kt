package com.prslc.zhiflow.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.model.FeedItem
import com.prslc.zhiflow.data.service.getRecommendFeed
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    var feedItems = mutableStateListOf<FeedItem>()
    var isRefreshing by mutableStateOf(false)
    var isNextLoading by mutableStateOf(false)
    private var nextPageUrl: String? = null

    init {
        refresh()
    }

    fun refresh() {
        if (isRefreshing) return
        viewModelScope.launch {
            try {
                isRefreshing = true
                val response = getRecommendFeed(limit = 10, nextUrl = null)

                if (response != null) {
                    feedItems.clear()
                    val items = response.data.filter { it.target != null }
                    feedItems.addAll(items)

                    nextPageUrl = response.paging.next
                }
            } catch (e: Exception) {
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
                e.printStackTrace()
            } finally {
                isNextLoading = false
            }
        }
    }
}