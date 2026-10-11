package com.prslc.zhiflow.ui.page.comment

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.offMain
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.data.dto.CommentDto
import com.prslc.zhiflow.data.mapper.toDto
import com.prslc.zhiflow.data.model.comment.ZhihuComment
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.data.repository.CommentRepository
import kotlinx.coroutines.launch

@Immutable
sealed interface CommentUiEvent {
    data object DismissSheet : CommentUiEvent

    data object BackToMain : CommentUiEvent

    data object LoadMoreReplies : CommentUiEvent

    data object ActionErrorShown : CommentUiEvent

    data class LoadRootComments(val id: String, val contentType: ContentType) : CommentUiEvent

    data class NavigatedToUser(val userId: String) : CommentUiEvent

    data class ToggleLike(val commentId: String) : CommentUiEvent

    data class ShowAuthor(val urlToken: String) : CommentUiEvent

    data class LoadChildComments(val rootComment: CommentDto) : CommentUiEvent

    /** What the reader has typed into the box. */
    data class DraftChanged(val text: String) : CommentUiEvent

    /** Sends what is in the box: a comment on the content, or a reply while replies are open. */
    data class SubmitComment(val id: String, val contentType: ContentType) : CommentUiEvent

    /**
     * Sends what is in a passage's box. The anchor is the passage the panel is about, which is
     * what the comment is filed under.
     */
    data class SubmitSegmentComment(
        val id: String,
        val contentType: ContentType,
        val anchor: SegmentLikeTarget,
    ) : CommentUiEvent
}

class CommentViewModel(private val repository: CommentRepository) : ViewModel() {

    @Stable
    data class CommentUiState(
        val isLoading: Boolean = false,
        val comments: List<CommentUiModel> = emptyList(),
        val totalCount: Int = 0,
        val offset: String = "",
        val hasMore: Boolean = true,
        val error: ApiException? = null,
        val actionError: ApiException? = null,
        val navigateToUser: String? = null,
        /** What is in the box, which the two panes share. */
        val draft: String = "",
        val isSubmitting: Boolean = false,
        /** The comment just posted, so the list can bring it into view; null until one is. */
        val lastPostedId: String? = null,
    )

    @Stable
    data class ChildCommentUiState(
        val isLoading: Boolean = false,
        val comments: List<CommentUiModel> = emptyList(),
        val rootComment: CommentUiModel? = null,
        val offset: String = "",
        val hasMore: Boolean = true,
        val error: ApiException? = null,
        val isDetailMode: Boolean = false,
        /** The reply just posted; see [CommentUiState.lastPostedId]. */
        val lastPostedId: String? = null,
    )

    @Stable
    data class CommentUiModel(
        val comment: CommentDto,
    )

    @Stable
    data class SegmentUiState(
        val isLoading: Boolean = false,
        val comments: List<CommentUiModel> = emptyList(),
        val totalCount: Int = 0,
        val offset: String = "",
        val hasMore: Boolean = true,
        val error: ApiException? = null,
        /** The comment just posted; see [CommentUiState.lastPostedId]. */
        val lastPostedId: String? = null,
    )

    var uiState by mutableStateOf(CommentUiState())
        private set

    var childUiState by mutableStateOf(ChildCommentUiState())
        private set

    var segmentUiState by mutableStateOf(SegmentUiState())
        private set

    private var lastLoadedAnswerId: String? = null
    private var lastContentType: ContentType? = null
    private var lastSegmentId: String? = null
    private val pendingReactions = mutableSetOf<String>()

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
                error = null,
            )
        } else {
            uiState = uiState.copy(isLoading = true, error = null)
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
                        error = null,
                    )
                }.onApiFailure { error ->
                    uiState = uiState.copy(isLoading = false, error = error)
                }
        }
    }

    fun loadMoreReplies() {
        childUiState.rootComment?.comment?.let { loadChildComments(it, forceRefresh = false) }
    }

    /**
     * Loads the comments left on one passage.
     *
     * @param segmentId The shared segment id its `seg_like` mark carries — not the `resource_id` of
     *   any comment in the answer, which names the comment's own segment record.
     */
    fun loadSegmentComments(
        id: String,
        contentType: ContentType,
        segmentId: String,
        forceRefresh: Boolean = false,
    ) {
        val isNewOrRefresh = forceRefresh || segmentId != lastSegmentId
        if (!isNewOrRefresh && (segmentUiState.isLoading || !segmentUiState.hasMore)) return

        if (isNewOrRefresh) {
            lastSegmentId = segmentId
            segmentUiState = SegmentUiState(isLoading = true)
        } else {
            segmentUiState = segmentUiState.copy(isLoading = true, error = null)
        }

        viewModelScope.launch {
            repository.getSegmentComments(id, contentType, segmentId, segmentUiState.offset)
                .onSuccess { response ->
                    val processed = response.data.toUiModels()
                    val nextOffset =
                        response.paging?.next?.toUri()?.getQueryParameter("offset") ?: ""

                    segmentUiState = segmentUiState.copy(
                        comments = if (isNewOrRefresh) {
                            processed
                        } else {
                            segmentUiState.comments + processed
                        },
                        totalCount = response.counts.total,
                        offset = nextOffset,
                        hasMore = response.paging?.isEnd == false,
                        isLoading = false,
                        error = null,
                    )
                }.onApiFailure { error ->
                    segmentUiState = segmentUiState.copy(isLoading = false, error = error)
                }
        }
    }

    fun loadChildComments(rootComment: CommentDto, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh) {
                val rootUiModel = CommentUiModel(rootComment)
                childUiState = ChildCommentUiState(
                    rootComment = rootUiModel,
                    isDetailMode = true,
                    isLoading = true,
                    comments = emptyList(),
                    hasMore = true,
                    offset = "",
                )
            }

            if (childUiState.isLoading && !forceRefresh) return@launch
            if (!forceRefresh) childUiState = childUiState.copy(isLoading = true, error = null)

            val currentOffset = if (forceRefresh) "" else childUiState.offset
            repository.getChildComments(rootComment.id, currentOffset).onSuccess { response ->
                val processed = response.data.toUiModels()
                val nextOffset =
                    response.paging?.next?.toUri()?.getQueryParameter("offset") ?: ""
                val hasNext = response.paging?.isEnd == false

                childUiState = childUiState.copy(
                    comments = if (forceRefresh) processed else childUiState.comments + processed,
                    offset = nextOffset,
                    hasMore = hasNext,
                    isLoading = false,
                )
            }.onApiFailure { error ->
                childUiState = childUiState.copy(isLoading = false, error = error)
            }
        }
    }

    /**
     * Toggle comment like with optimistic UI.
     *
     * Uses [pendingReactions] to debounce rapid taps. Rolls back on failure.
     */
    fun toggleLike(commentId: String) {
        if (pendingReactions.contains(commentId)) return

        val targetModel = uiState.comments.find { it.comment.id == commentId }
            ?: childUiState.comments.find { it.comment.id == commentId }
            ?: segmentUiState.comments.find { it.comment.id == commentId }
            ?: childUiState.rootComment?.takeIf { it.comment.id == commentId } ?: return

        val isCurrentlyActive = targetModel.comment.liked
        val shouldBeActive = !isCurrentlyActive

        updateLocalStatus(commentId, shouldBeActive)
        pendingReactions.add(commentId)

        viewModelScope.launch {
            repository.toggleLike(commentId, shouldBeActive).onApiFailure { error ->
                updateLocalStatus(commentId, isCurrentlyActive)
                uiState = uiState.copy(actionError = error)
            }
            pendingReactions.remove(commentId)
        }
    }

    /**
     * Keeps what the reader is typing. The box is theirs until it is sent or the sheet closes.
     */
    fun onDraftChange(text: String) {
        uiState = uiState.copy(draft = text)
    }

    /**
     * Posts what is in the box.
     *
     * While one comment's replies are open the words answer that comment; with the root list up
     * they are a comment on the content itself. No-op on an empty box, or while one is in flight —
     * a failure leaves the words where they are and says so through the sheet's own notice.
     *
     * @param id The content the comments belong to.
     * @param contentType What that content is.
     */
    fun submitComment(id: String, contentType: ContentType) {
        post(id, contentType, anchor = null)
    }

    /**
     * Posts what is in a passage's box, which is the same act on a comment filed under that
     * passage rather than under the content.
     *
     * @param id The content the passage belongs to.
     * @param contentType What that content is.
     * @param anchor The passage the panel is about.
     */
    fun submitSegmentComment(id: String, contentType: ContentType, anchor: SegmentLikeTarget) {
        post(id, contentType, anchor)
    }

    private fun post(id: String, contentType: ContentType, anchor: SegmentLikeTarget?) {
        val text = uiState.draft.trim()
        if (text.isEmpty() || uiState.isSubmitting) return

        val replyTo = childUiState.takeIf { it.isDetailMode }?.rootComment?.comment
        uiState = uiState.copy(isSubmitting = true)

        viewModelScope.launch {
            val posted = if (anchor == null) {
                repository.postComment(id, contentType, text, replyTo?.id)
            } else {
                repository.postSegmentComment(id, contentType, text, anchor, replyTo?.id)
            }

            posted.onSuccess { comment -> onPosted(comment, replyTo, anchor) }
                .onApiFailure { error ->
                    uiState = uiState.copy(isSubmitting = false, actionError = error)
                }
        }
    }

    /**
     * Puts the posted comment where it belongs: a reply joins the end of the list it answered, and
     * a comment goes to the top of the list it was written in, which is where the reader is
     * looking. The count of the comment that was answered goes up with it, wherever it is drawn.
     */
    private fun onPosted(posted: CommentDto, replyTo: CommentDto?, anchor: SegmentLikeTarget?) {
        val model = CommentUiModel(posted)

        if (replyTo == null) {
            uiState = uiState.copy(draft = "", isSubmitting = false)

            if (anchor == null) {
                uiState = uiState.copy(
                    comments = listOf(model) + uiState.comments,
                    totalCount = uiState.totalCount + 1,
                    lastPostedId = posted.id,
                )
            } else {
                segmentUiState = segmentUiState.copy(
                    comments = listOf(model) + segmentUiState.comments,
                    totalCount = segmentUiState.totalCount + 1,
                    lastPostedId = posted.id,
                )
            }
            return
        }

        val answered: (CommentUiModel) -> CommentUiModel = { entry ->
            if (entry.comment.id == replyTo.id) {
                entry.copy(comment = entry.comment.copy(childCount = entry.comment.childCount + 1))
            } else {
                entry
            }
        }

        childUiState = childUiState.copy(
            comments = childUiState.comments + model,
            rootComment = childUiState.rootComment?.let(answered),
            lastPostedId = posted.id,
        )
        uiState = uiState.copy(
            comments = uiState.comments.map(answered),
            draft = "",
            isSubmitting = false,
        )
    }

    /**
     * Sends the reply pane back to the root list. The root comment is kept: the pane is still
     * composed while it slides out, and a list that lost the comment would drop the card under the
     * reader's eyes and pull its replies up in that very frame. Opening a comment replaces the
     * whole state, so nothing stale can reach the pane.
     */
    fun backToMain() {
        childUiState = childUiState.copy(isDetailMode = false)
    }

    fun showAuthor(urlToken: String) {
        uiState = uiState.copy(navigateToUser = urlToken)
    }

    fun onNavigated() {
        uiState = uiState.copy(navigateToUser = null)
    }

    fun onActionErrorShown() {
        uiState = uiState.copy(actionError = null)
    }

    /** Reset all comment state when the bottom sheet is dismissed. */
    fun onSheetDismissed() {
        uiState = CommentUiState()
        childUiState = ChildCommentUiState()
        segmentUiState = SegmentUiState()
        lastLoadedAnswerId = null
        lastContentType = null
        lastSegmentId = null
        pendingReactions.clear()
    }

    /**
     * Parses each comment's HTML into what the list draws.
     *
     * Through [offMain] because it is a page's worth of HTML to walk, and it lands while the sheet
     * is still animating in -- the sheet's own effect is what asks for the page.
     */
    private suspend fun List<ZhihuComment>.toUiModels(): List<CommentUiModel> =
        offMain { map { CommentUiModel(it.toDto()) } }

    private fun updateLocalStatus(id: String, active: Boolean) {
        val mapper = { model: CommentUiModel ->
            if (model.comment.id == id) {
                val updatedComment = model.comment.copy(
                    liked = active,
                    likeCount = if (active) {
                        model.comment.likeCount + 1
                    } else {
                        (model.comment.likeCount - 1).coerceAtLeast(0)
                    },
                )
                model.copy(comment = updatedComment)
            } else {
                model
            }
        }

        uiState = uiState.copy(comments = uiState.comments.map(mapper))
        childUiState = childUiState.copy(
            rootComment = childUiState.rootComment?.let(mapper),
            comments = childUiState.comments.map(mapper),
        )
        segmentUiState = segmentUiState.copy(comments = segmentUiState.comments.map(mapper))
    }
}
