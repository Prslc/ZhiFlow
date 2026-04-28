package com.prslc.zhiflow.ui.page.comment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.toApiException
import com.prslc.zhiflow.data.model.CommentContent
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ZhihuComment
import com.prslc.zhiflow.data.repository.CommentRepository
import com.prslc.zhiflow.parser.CommentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class CommentViewModel(private val repository: CommentRepository) : ViewModel() {

    data class CommentUiState(
        val isLoading: Boolean = false,
        val comments: List<CommentUiModel> = emptyList(),
        val totalCount: Int = 0,
        val offset: String = "",
        val hasMore: Boolean = true,
        val error: ApiException? = null,
        val isLightboxVisible: Boolean = false,
        val selectedImageUrls: List<String> = emptyList(),
        val initialImageIndex: Int = 0,
        val navigateToUser: String? = null
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
            map { CommentUiModel(it, CommentParser.parse(it.content)) }
        }

    private suspend fun ZhihuComment.toUiModel(): CommentUiModel =
        withContext(Dispatchers.Default) {
            CommentUiModel(this@toUiModel, CommentParser.parse(this@toUiModel.content))
        }

    fun loadComments(answerId: String, contentType: ContentType, forceRefresh: Boolean = false) {
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
                    if (e is CancellationException) throw e
                    uiState = uiState.copy(isLoading = false, error = e.toApiException())
                }
        }
    }

    fun loadMoreReplies() {
        childUiState.rootComment?.comment?.let { loadChildComments(it, forceRefresh = false) }
    }

    fun loadChildComments(rootComment: ZhihuComment, forceRefresh: Boolean = false) {
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
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    childUiState = childUiState.copy(isLoading = false)
                }
        }
    }

    fun toggleLike(commentId: String) {
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

    fun openImage(url: String) {
        uiState = uiState.copy(
            selectedImageUrls = listOf(url),
            initialImageIndex = 0,
            isLightboxVisible = true
        )
    }

    fun closeImage() {
        uiState = uiState.copy(isLightboxVisible = false, selectedImageUrls = emptyList())
    }

    fun backToMain() {
        childUiState = childUiState.copy(isDetailMode = false, rootComment = null)
    }

    fun showAuthor(urlToken: String) {
        uiState = uiState.copy(navigateToUser = urlToken)
    }

    fun onNavigated() {
        uiState = uiState.copy(navigateToUser = null)
    }

    fun onSheetDismissed() {
        uiState = CommentUiState()
        childUiState = ChildCommentUiState()
        lastLoadedAnswerId = null
        lastContentType = null
        pendingReactions.clear()
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
}
