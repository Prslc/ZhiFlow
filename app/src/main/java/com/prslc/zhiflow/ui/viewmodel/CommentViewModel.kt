package com.prslc.zhiflow.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.model.ZhihuComment
import com.prslc.zhiflow.data.service.getRootComments
import kotlinx.coroutines.launch

class CommentViewModel : ViewModel() {

    data class CommentUiState(
        val isLoading: Boolean = false,
        val comments: List<ZhihuComment> = emptyList(),
        val totalCount: Int = 0,
        val offset: String = "",
        val hasMore: Boolean = true,
        val error: Throwable? = null
    )

    var uiState by mutableStateOf(CommentUiState())
        private set

    // load
    fun loadComments(answerId: String, forceRefresh: Boolean = false) {
        if (forceRefresh) {
            uiState = CommentUiState()
        }

        if (uiState.isLoading || (!uiState.hasMore && !forceRefresh)) return

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            try {
                val response = getRootComments(
                    answerId = answerId,
                    offset = uiState.offset,
                    limit = 20
                )

                if (response != null) {
                    val newComments = uiState.comments + response.data

                    val nextOffset =
                        response.paging?.next?.toUri()?.getQueryParameter("offset") ?: ""
                    val hasNext = response.paging?.isEnd == false

                    uiState = uiState.copy(
                        comments = newComments,
                        totalCount = response.counts.total,
                        offset = nextOffset,
                        hasMore = hasNext,
                        isLoading = false
                    )
                }
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    fun resetState() {
        uiState = CommentUiState()
    }
}