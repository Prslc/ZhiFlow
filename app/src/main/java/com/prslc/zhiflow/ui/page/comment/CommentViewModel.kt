package com.prslc.zhiflow.ui.page.comment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.model.CommentContent
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ZhihuComment
import com.prslc.zhiflow.data.repository.CommentRepository
import com.prslc.zhiflow.parser.commentParse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface CommentEvent {
    data class LoadComments(val id: String, val contentType: ContentType) : CommentEvent
    data class LoadChildComments(val root: ZhihuComment, val forceRefresh: Boolean) : CommentEvent
    data object LoadMoreReplies : CommentEvent
    data class ToggleLike(val commentId: String) : CommentEvent
    data class OpenImage(val url: String) : CommentEvent
    data object CloseImage : CommentEvent
    data object BackToMain : CommentEvent
    data object Dismiss : CommentEvent
    data class ShowAuthor(val urlToken: String) : CommentEvent
}

class CommentViewModel(private val repository: CommentRepository) : ViewModel() {

    data class CommentUiState(
        val isLoading: Boolean = false,
        val comments: List<CommentUiModel> = emptyList(),
        val totalCount: Int = 0,
        val offset: String = "",
        val hasMore: Boolean = true,
        val error: Throwable? = null,
        val isLightboxVisible: Boolean = false,
        val selectedImageUrls: List<String> = emptyList(),
        val initialImageIndex: Int = 0
    )

    data class ChildCommentUiState(
        val isLoading: Boolean = false,
        val comments: List<CommentUiModel> = emptyList(),
        val rootComment: CommentUiModel? = null,
        val offset: String = "",
        val hasMore: Boolean = true,
        val isDetailMode: Boolean = false
    )

    data class CommentUiModel(
        val comment: ZhihuComment,
        val parsedContent: CommentContent
    )

    var uiState by mutableStateOf(CommentUiState())
        private set

    var childUiState by mutableStateOf(ChildCommentUiState())
        private set

    private var lastLoadedAnswerId: String? = null
    private var lastContentType: ContentType? = null
    private val pendingReactions = mutableSetOf<String>()

    private suspend fun List<ZhihuComment>.toUiModels(): List<CommentUiModel> =
        withContext(Dispatchers.Default) {
            map { CommentUiModel(it, commentParse(it.content)) }
        }

    private suspend fun ZhihuComment.toUiModel(): CommentUiModel =
        withContext(Dispatchers.Default) {
            CommentUiModel(this@toUiModel, commentParse(this@toUiModel.content))
        }

    fun onEvent(event: CommentEvent) {
        when (event) {
            is CommentEvent.LoadComments -> loadComments(event.id, event.contentType)
            is CommentEvent.LoadChildComments -> loadChildComments(event.root, event.forceRefresh)
            is CommentEvent.LoadMoreReplies -> loadMoreReplies()
            is CommentEvent.ToggleLike -> updateCommentReaction(event.commentId)
            is CommentEvent.OpenImage -> openImageLightbox(event.url)
            is CommentEvent.CloseImage -> closeImageLightbox()
            is CommentEvent.BackToMain -> backToMain()
            is CommentEvent.Dismiss -> onSheetDismissed()
            is CommentEvent.ShowAuthor -> { /* TODO */ }
        }
    }

    private fun openImageLightbox(url: String) {
        uiState = uiState.copy(
            selectedImageUrls = listOf(url),
            initialImageIndex = 0,
            isLightboxVisible = true
        )
    }

    private fun closeImageLightbox() {
        uiState = uiState.copy(isLightboxVisible = false, selectedImageUrls = emptyList())
    }

    private fun loadComments(answerId: String, contentType: ContentType, forceRefresh: Boolean = false) {
        val isNewOrRefresh = forceRefresh || answerId != lastLoadedAnswerId
        if (!forceRefresh && !isNewOrRefresh && uiState.isLoading) return

        if (isNewOrRefresh) {
            lastLoadedAnswerId = answerId
            lastContentType = contentType
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
                    val processed = response.data.toUiModels()
                    val nextOffset =
                        response.paging?.next?.toUri()?.getQueryParameter("offset") ?: ""
                    val hasNext = response.paging?.isEnd == false

                    uiState = uiState.copy(
                        comments = if (isNewOrRefresh) processed else uiState.comments + processed,
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

    private fun loadMoreReplies() {
        childUiState.rootComment?.comment?.let { loadChildComments(it, forceRefresh = false) }
    }

    private fun loadChildComments(rootComment: ZhihuComment, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                val rootUiModel = rootComment.toUiModel()
                childUiState = ChildCommentUiState(
                    rootComment = rootUiModel,
                    isDetailMode = true,
                    isLoading = true,
                    comments = emptyList(),
                    hasMore = true,
                    offset = ""
                )
            }

            if (childUiState.isLoading && !forceRefresh) return@launch
            if (!forceRefresh) childUiState = childUiState.copy(isLoading = true)

            val currentOffset = if (forceRefresh) "" else childUiState.offset
            repository.getChildComments(rootComment.id, currentOffset)
                .onSuccess { response ->
                    val processed = response.data.toUiModels()
                    val nextOffset =
                        response.paging?.next?.toUri()?.getQueryParameter("offset") ?: ""
                    val hasNext = response.paging?.isEnd == false

                    childUiState = childUiState.copy(
                        comments = if (forceRefresh) processed else childUiState.comments + processed,
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

    private fun updateCommentReaction(commentId: String) {
        if (pendingReactions.contains(commentId)) return

        val targetModel = uiState.comments.find { it.comment.id == commentId }
            ?: childUiState.comments.find { it.comment.id == commentId }
            ?: childUiState.rootComment?.takeIf { it.comment.id == commentId }
            ?: return

        val isCurrentlyActive = targetModel.comment.liked
        val shouldBeActive = !isCurrentlyActive

        updateLocalStatus(commentId, shouldBeActive)
        pendingReactions.add(commentId)

        viewModelScope.launch {
            try {
                val success = repository.toggleLike(commentId, shouldBeActive)
                if (!success) {
                    updateLocalStatus(commentId, isCurrentlyActive)
                }
            } finally {
                pendingReactions.remove(commentId)
            }
        }
    }

    private fun updateLocalStatus(id: String, active: Boolean) {
        val mapper = { model: CommentUiModel ->
            if (model.comment.id == id) {
                val updatedComment = model.comment.copy(
                    liked = active,
                    likeCount = if (active) model.comment.likeCount + 1
                    else (model.comment.likeCount - 1).coerceAtLeast(0)
                )
                model.copy(comment = updatedComment)
            } else model
        }

        uiState = uiState.copy(comments = uiState.comments.map(mapper))
        childUiState = childUiState.copy(
            rootComment = childUiState.rootComment?.let(mapper),
            comments = childUiState.comments.map(mapper)
        )
    }

    private fun backToMain() {
        childUiState = childUiState.copy(isDetailMode = false, rootComment = null)
    }

    fun onSheetDismissed() {
        uiState = CommentUiState()
        childUiState = ChildCommentUiState()
        lastLoadedAnswerId = null
        lastContentType = null
        pendingReactions.clear()
    }
}
