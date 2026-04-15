package com.prslc.zhiflow.ui.page.comment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ZhihuComment
import com.prslc.zhiflow.data.repository.CommentRepository
import kotlinx.coroutines.launch

class CommentViewModel(private val repository: CommentRepository) : ViewModel() {

    /**
     * UI state for the main comment list
     */
    data class CommentUiState(
        val isLoading: Boolean = false,
        val comments: List<ZhihuComment> = emptyList(),
        val totalCount: Int = 0,
        val offset: String = "",
        val hasMore: Boolean = true,
        val error: Throwable? = null,
        val isLightboxVisible: Boolean = false,
        val selectedImageUrls: List<String> = emptyList(),
        val initialImageIndex: Int = 0
    )

    /**
     * UI state for the child comment (replies) view
     */
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
        uiState = uiState.copy(
            selectedImageUrls = listOf(url),
            initialImageIndex = 0,
            isLightboxVisible = true
        )
    }

    fun closeImageLightbox() {
        uiState = uiState.copy(
            isLightboxVisible = false,
            selectedImageUrls = emptyList()
        )
    }

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
            repository.getRootComments(answerId, contentType, uiState.offset)
                .onSuccess { response ->
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
                }
                .onFailure { e ->
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

            val currentOffset = if (forceRefresh) "" else childUiState.offset
            repository.getChildComments(rootComment.id, currentOffset)
                .onSuccess { response ->
                    val nextOffset = response.paging?.next?.toUri()?.getQueryParameter("offset") ?: ""
                    val hasNext = response.paging?.isEnd == false

                    childUiState = childUiState.copy(
                        comments = if (forceRefresh) response.data else childUiState.comments + response.data,
                        offset = nextOffset,
                        hasMore = hasNext,
                        isLoading = false
                    )
                }
                .onFailure {
                    childUiState = childUiState.copy(isLoading = false)
                }
        }
    }

    fun updateCommentReaction(commentId: String, isLikeAction: Boolean) {
        // Find the target comment in either root or child lists
        val targetComment = uiState.comments.find { it.id == commentId }
            ?: childUiState.comments.find { it.id == commentId }
            ?: childUiState.rootComment?.takeIf { it.id == commentId }
            ?: return

        // Business logic: if currently liked, this action will "unlike" it
        val isCurrentlyActive = if (isLikeAction) targetComment.liked else false
        val shouldBeActive = !isCurrentlyActive

        // Optimistic UI update
        updateLocalStatus(commentId, isLikeAction, shouldBeActive)

        viewModelScope.launch {
            val success = repository.toggleLike(commentId, shouldBeActive)
            if (!success) {
                // Rollback on failure
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
                    comment // TODO: Add support for other reactions
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