package com.prslc.zhiflow.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.model.ZhihuComment
import com.prslc.zhiflow.data.service.getChildComments
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

    data class ChildCommentUiState(
        val isLoading: Boolean = false,
        val comments: List<ZhihuComment> = emptyList(),
        val rootComment: ZhihuComment? = null,
        val offset: String = "",
        val hasMore: Boolean = true,
        val showSheet: Boolean = false
    )

    var uiState by mutableStateOf(CommentUiState())
        private set

    var childUiState by mutableStateOf(ChildCommentUiState())
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

    fun loadChildComments(rootComment: ZhihuComment, forceRefresh: Boolean = false) {
        if (forceRefresh) {
            childUiState = ChildCommentUiState(
                rootComment = rootComment,
                showSheet = true,
                isLoading = true,
                comments = emptyList(),
                hasMore = true,
                offset = ""
            )
        }

        if (childUiState.isLoading && !forceRefresh || (!childUiState.hasMore && !forceRefresh)) return

        viewModelScope.launch {
            childUiState = childUiState.copy(isLoading = true)

            try {
                val currentOffset = if (forceRefresh) "" else childUiState.offset
                val response = getChildComments(rootCommentId = rootComment.id, offset = currentOffset)

                if (response != null) {
                    val paging = response.paging
                    val nextOffset = paging?.next?.toUri()?.getQueryParameter("offset") ?: ""
                    val hasNext = paging?.isEnd == false

                    childUiState = childUiState.copy(
                        comments = if (forceRefresh) response.data else childUiState.comments + response.data,
                        offset = nextOffset,
                        hasMore = hasNext,
                        isLoading = false
                    )
                }
            } finally {
                childUiState = childUiState.copy(isLoading = false)
            }
        }
    }

    fun dismissChildSheet() {
        childUiState = childUiState.copy(showSheet = false)
    }

    fun resetState() {
        uiState = CommentUiState()
    }
}