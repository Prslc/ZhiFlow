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
        val isDetailMode: Boolean = false
    )

    var uiState by mutableStateOf(CommentUiState())
        private set

    var childUiState by mutableStateOf(ChildCommentUiState())
        private set

    private var lastLoadedAnswerId: String? = null

    // load
    fun loadComments(answerId: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && answerId == lastLoadedAnswerId && uiState.comments.isNotEmpty()) return
        if (uiState.isLoading && !forceRefresh) return

        val isNewOrRefresh = forceRefresh || answerId != lastLoadedAnswerId
        if (isNewOrRefresh) {
            lastLoadedAnswerId = answerId
            uiState = uiState.copy(
                isLoading = true,
                comments = emptyList(),
                offset = "",
                hasMore = true
            )
        } else {
            uiState = uiState.copy(isLoading = true)
        }

        viewModelScope.launch {
            try {
                val response = getRootComments(answerId, uiState.offset)
                if (response != null) {
                    val nextOffset = response.paging?.next?.toUri()?.getQueryParameter("offset") ?: ""
                    val hasNext = response.paging?.isEnd == false

                    uiState = uiState.copy(
                        comments = if (isNewOrRefresh) response.data else (uiState.comments + response.data).distinctBy { it.id },
                        totalCount = response.counts.total,
                        offset = nextOffset,
                        hasMore = hasNext,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, error = e)
            }
        }
    }

    fun loadChildComments(rootComment: ZhihuComment, forceRefresh: Boolean = false) {
        if (forceRefresh) {
            childUiState = ChildCommentUiState(
                rootComment = rootComment,
                isDetailMode = true,
                isLoading = true,
                comments = emptyList(),
                hasMore = true,
                offset = ""
            )
        }

        if (childUiState.isLoading && !forceRefresh) return

        viewModelScope.launch {
            if (!forceRefresh) childUiState = childUiState.copy(isLoading = true)

            try {
                val currentOffset = if (forceRefresh) "" else childUiState.offset
                val response = getChildComments(rootCommentId = rootComment.id, offset = currentOffset)

                if (response != null) {
                    val nextOffset = response.paging?.next?.toUri()?.getQueryParameter("offset") ?: ""
                    val hasNext = response.paging?.isEnd == false

                    childUiState = childUiState.copy(
                        comments = if (forceRefresh) response.data else childUiState.comments + response.data,
                        offset = nextOffset,
                        hasMore = hasNext,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                childUiState = childUiState.copy(isLoading = false)
            }
        }
    }

    fun backToMain() {
        childUiState = childUiState.copy(
            isDetailMode = false,
            rootComment = null
        )
    }

    fun resetState() {
        uiState = CommentUiState()
    }

    fun onSheetDismissed() {
        childUiState = ChildCommentUiState()
    }
}
