package com.prslc.zhiflow.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ZhihuComment
import com.prslc.zhiflow.data.service.commentReaction
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
        val error: Throwable? = null,
        val selectedImageUrl: String? = null
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


    fun openImageLightbox(url: String) {
        uiState = uiState.copy(selectedImageUrl = url)
    }

    fun closeImageLightbox() {
        uiState = uiState.copy(selectedImageUrl = null)
    }

    // load
    fun loadComments(answerId: String, contentType: ContentType, forceRefresh: Boolean = false) {
        val isNewOrRefresh = forceRefresh || answerId != lastLoadedAnswerId

        if (!forceRefresh && !isNewOrRefresh && uiState.isLoading) return

        if (isNewOrRefresh) {
            lastLoadedAnswerId = answerId
            uiState = uiState.copy(
                isLoading = true,
                comments = emptyList(),
                offset = "",
                hasMore = true,
                error = null
            )
        } else {
            uiState = uiState.copy(isLoading = true)
        }

        viewModelScope.launch {
            try {
                val response = getRootComments(answerId, contentType, uiState.offset)
                if (response != null) {
                    val nextOffset = response.paging?.next?.toUri()?.getQueryParameter("offset") ?: ""
                    val hasNext = response.paging?.isEnd == false

                    val updatedComments = if (isNewOrRefresh) response.data else uiState.comments + response.data

                    uiState = uiState.copy(
                        comments = updatedComments,
                        totalCount = response.counts.total,
                        offset = nextOffset,
                        hasMore = hasNext,
                        isLoading = false,
                        error = null
                    )
                } else {
                    uiState = uiState.copy(isLoading = false)
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

    fun updateCommentReaction(commentId: String, isLikeAction: Boolean) {
        val targetComment = uiState.comments.find { it.id == commentId }
            ?: childUiState.comments.find { it.id == commentId }
            ?: childUiState.rootComment?.takeIf { it.id == commentId }
            ?: return

        val action = if (isLikeAction) "like" else "unlike"
        val isCurrentlyActive = if (isLikeAction) targetComment.liked else false
        val method = if (isCurrentlyActive) "DELETE" else "POST"

        updateLocalStatus(commentId, isLikeAction, !isCurrentlyActive)

        viewModelScope.launch {
            val success = commentReaction(commentId, action, method)
            if (!success) {
                updateLocalStatus(commentId, isLikeAction, isCurrentlyActive)
            }
        }
    }

    private fun updateLocalStatus(id: String, isLike: Boolean, active: Boolean) {
        val mapper = { comment: ZhihuComment ->
            if (comment.id == id) {
                if (isLike) {
                    comment.copy(
                        liked = active,
                        likeCount = if (active) comment.likeCount + 1 else (comment.likeCount - 1).coerceAtLeast(0)
                    )
                } else {
                    // TODO
                    comment
                }
            } else comment
        }

        uiState = uiState.copy(comments = uiState.comments.map(mapper))
        childUiState = childUiState.copy(
            rootComment = childUiState.rootComment?.let(mapper),
            comments = childUiState.comments.map(mapper)
        )
    }

    fun backToMain() {
        childUiState = childUiState.copy(
            isDetailMode = false,
            rootComment = null,
            comments = emptyList()
        )
    }

    fun resetState() {
        uiState = CommentUiState()
    }

    fun onSheetDismissed() {
        uiState = CommentUiState()
        childUiState = ChildCommentUiState()
        lastLoadedAnswerId = null
    }
}
