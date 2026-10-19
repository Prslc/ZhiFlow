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
import com.prslc.zhiflow.core.utils.compose.withPage
import com.prslc.zhiflow.data.dto.CommentDto
import com.prslc.zhiflow.data.mapper.toDto
import com.prslc.zhiflow.data.model.comment.ZhihuComment
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.data.model.content.ZhihuContent
import com.prslc.zhiflow.data.repository.CommentRepository
import kotlinx.coroutines.Job
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

    data class ToggleDislike(val commentId: String) : CommentUiEvent

    data class ShowAuthor(val urlToken: String) : CommentUiEvent

    data class LoadChildComments(val rootComment: CommentDto) : CommentUiEvent

    /**
     * The reader asked the box to answer this comment rather than to open anything: the box comes
     * up pointing at it, in whichever pane the comment is drawn.
     */
    data class ReplyTo(val comment: CommentDto) : CommentUiEvent

    /** The reader asked to comment on the content itself: the box comes up answering no one. */
    data object OpenComposer : CommentUiEvent

    /** The reader put the box away without sending. */
    data object CloseComposer : CommentUiEvent

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
        /**
         * Whether the box is up. A comment is written only when the reader asks for one — a row's
         * reply, or the way in to a comment of their own — so the box is down for a sheet that was
         * merely opened.
         */
        val isComposerOpen: Boolean = false,
        /**
         * Whether the comment section takes a written comment at all, which its own payload says.
         * True until it says otherwise, so a section still being read is not taken for a closed
         * one.
         */
        val canWrite: Boolean = true,
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
        /**
         * The comment the box will answer, where the reader asked to answer one: a reply to a reply
         * is written in the pane that reply is already in. Null while the box answers nobody.
         */
        val replyTarget: CommentDto? = null,
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
    private var lastChildCommentId: String? = null
    private var segmentJob: Job? = null
    private var childJob: Job? = null
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
                isComposerOpen = false,
            )
            // A list read afresh is not the reader's last visit to it: a box left answering some
            // comment of the old one is put away, and the old one's target with it.
            childUiState = childUiState.copy(replyTarget = null)
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
                        comments = uiState.comments.withPage(
                            processed,
                            firstPage = isNewOrRefresh,
                        ) { it.comment.id },
                        totalCount = response.counts.total,
                        offset = nextOffset,
                        hasMore = hasNext,
                        isLoading = false,
                        error = null,
                        // A page that says nothing leaves the section as the one before it left it.
                        canWrite = response.editStatus?.canReply ?: uiState.canWrite,
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
            // A passage the reader has moved on from must not be answered into the one they are
            // looking at now: both write the same state, so whichever finished last would be what
            // they read -- another passage's comments, and its count beside them.
            segmentJob?.cancel()
            lastSegmentId = segmentId
            segmentUiState = SegmentUiState(isLoading = true)
        } else {
            segmentUiState = segmentUiState.copy(isLoading = true, error = null)
        }

        segmentJob = viewModelScope.launch {
            repository.getSegmentComments(id, contentType, segmentId, segmentUiState.offset)
                .onSuccess { response ->
                    val processed = response.data.toUiModels()
                    val nextOffset =
                        response.paging?.next?.toUri()?.getQueryParameter("offset") ?: ""

                    segmentUiState = segmentUiState.copy(
                        comments = segmentUiState.comments.withPage(
                            processed,
                            firstPage = isNewOrRefresh,
                        ) { it.comment.id },
                        totalCount = response.counts.total,
                        offset = nextOffset,
                        hasMore = response.paging?.isEnd == false,
                        isLoading = false,
                        error = null,
                    )
                    // The passage's comments are written under the same section as the content's.
                    response.editStatus?.let { edit ->
                        uiState = uiState.copy(canWrite = edit.canReply)
                    }
                }.onApiFailure { error ->
                    segmentUiState = segmentUiState.copy(isLoading = false, error = error)
                }
        }
    }

    /**
     * Points the box at the comment a tap means to answer, wherever the tap was, and brings the box
     * up: answering is the one thing the tap is for. Opening a comment's replies is the block's
     * job, which is the only thing in a row that leads anywhere.
     *
     * The box then answers that comment -- in the pane the comment belongs to, which is the one the
     * reader is looking at.
     */
    fun replyTo(comment: CommentDto) {
        childUiState = childUiState.copy(replyTarget = comment)
        uiState = uiState.copy(isComposerOpen = true)
    }

    /** Brings the box up answering no one: a comment on the content itself. */
    fun openComposer() {
        childUiState = childUiState.copy(replyTarget = null)
        uiState = uiState.copy(isComposerOpen = true)
    }

    /**
     * Puts the box away, and whom it was answering with it, so the next opening says afresh whom it
     * is addressed to. What was typed stays: a draft is the reader's until it is sent or the sheet
     * closes.
     */
    fun closeComposer() {
        uiState = uiState.copy(isComposerOpen = false)
        childUiState = childUiState.copy(replyTarget = null)
    }

    fun loadChildComments(rootComment: CommentDto, forceRefresh: Boolean = false) {
        // The pane's subject is the comment whose replies are open, and the two panes beside this
        // one track theirs the same way. An answer to the comment the reader has left must not be
        // written into the one they are reading now.
        val isNewOrRefresh = forceRefresh || rootComment.id != lastChildCommentId
        if (isNewOrRefresh) {
            childJob?.cancel()
            lastChildCommentId = rootComment.id
        }

        childJob = viewModelScope.launch {
            if (isNewOrRefresh) {
                val rootUiModel = CommentUiModel(rootComment)
                childUiState = ChildCommentUiState(
                    rootComment = rootUiModel,
                    isDetailMode = true,
                    isLoading = true,
                    comments = emptyList(),
                    hasMore = true,
                    offset = "",
                )
                // The box was addressed from the list being left.
                uiState = uiState.copy(isComposerOpen = false)
            }

            if (childUiState.isLoading && !isNewOrRefresh) return@launch
            if (!isNewOrRefresh) childUiState = childUiState.copy(isLoading = true, error = null)

            val currentOffset = if (isNewOrRefresh) "" else childUiState.offset
            repository.getChildComments(rootComment.id, currentOffset).onSuccess { response ->
                val processed = response.data.toUiModels()
                val nextOffset =
                    response.paging?.next?.toUri()?.getQueryParameter("offset") ?: ""
                val hasNext = response.paging?.isEnd == false

                childUiState = childUiState.copy(
                    comments = childUiState.comments.withPage(
                        processed,
                        firstPage = isNewOrRefresh,
                    ) { it.comment.id },
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

        updateLocalLike(commentId, shouldBeActive)
        pendingReactions.add(commentId)

        viewModelScope.launch {
            repository.toggleLike(commentId, shouldBeActive).onApiFailure { error ->
                updateLocalLike(commentId, isCurrentlyActive)
                uiState = uiState.copy(actionError = error)
            }
            pendingReactions.remove(commentId)
        }
    }

    /**
     * Takes the reader's dislike off or puts it on, where the row keeps its place either way.
     *
     * A disliked comment is not hidden here: the server stops handing it back, so it is gone the
     * next time the list is read -- and until then the thumb is still there to take it back.
     * [pendingReactions] debounces rapid taps, and a failure is rolled back.
     */
    fun toggleDislike(commentId: String) {
        if (pendingReactions.contains(commentId)) return

        val targetModel = uiState.comments.find { it.comment.id == commentId }
            ?: childUiState.comments.find { it.comment.id == commentId }
            ?: segmentUiState.comments.find { it.comment.id == commentId }
            ?: childUiState.rootComment?.takeIf { it.comment.id == commentId } ?: return

        val isCurrentlyActive = targetModel.comment.disliked
        val shouldBeActive = !isCurrentlyActive

        updateLocalDislike(commentId, shouldBeActive)
        pendingReactions.add(commentId)

        viewModelScope.launch {
            repository.toggleDislike(commentId, shouldBeActive).onApiFailure { error ->
                updateLocalDislike(commentId, isCurrentlyActive)
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

        // What the box is answering: the comment the reader pointed it at. A box that answers no
        // one is a comment on the content itself.
        val replyTo = childUiState.replyTarget
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
            uiState = uiState.copy(draft = "", isSubmitting = false, isComposerOpen = false)

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
            // The answer is written; the box closes with the next opening told afresh.
            replyTarget = null,
            lastPostedId = posted.id,
        )
        uiState = uiState.copy(
            comments = uiState.comments.map(answered),
            draft = "",
            isSubmitting = false,
            isComposerOpen = false,
        )
    }

    /**
     * Sends the reply pane back to the root list. The root comment is kept: the pane is still
     * composed while it slides out, and a list that lost the comment would drop the card under the
     * reader's eyes and pull its replies up in that very frame. Opening a comment replaces the
     * whole state, so nothing stale can reach the pane.
     */
    fun backToMain() {
        childUiState = childUiState.copy(isDetailMode = false, replyTarget = null)
        // The box was addressed from the pane being left; the pane following it has not asked.
        uiState = uiState.copy(isComposerOpen = false)
    }

    fun showAuthor(urlToken: String) {
        uiState = uiState.copy(navigateToUser = urlToken)
    }

    fun onNavigated() {
        uiState = uiState.copy(navigateToUser = null)
    }

    fun consumeActionError() {
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

    /** Rewrites one comment wherever it can be drawn: the list, a pane's own comment, its replies, a passage's. */
    private fun mapEveryComment(update: (CommentUiModel) -> CommentUiModel) {
        uiState = uiState.copy(comments = uiState.comments.map(update))
        childUiState = childUiState.copy(
            rootComment = childUiState.rootComment?.let(update),
            comments = childUiState.comments.map(update),
        )
        segmentUiState = segmentUiState.copy(comments = segmentUiState.comments.map(update))
    }

    private fun updateLocalLike(id: String, active: Boolean) = mapEveryComment { model ->
        if (model.comment.id != id) return@mapEveryComment model
        model.copy(
            comment = model.comment.copy(
                liked = active,
                likeCount = if (active) model.comment.likeCount + 1 else (model.comment.likeCount - 1).coerceAtLeast(0),
            ),
        )
    }

    private fun updateLocalDislike(id: String, active: Boolean) = mapEveryComment { model ->
        if (model.comment.id != id) return@mapEveryComment model
        model.copy(comment = model.comment.copy(disliked = active))
    }
}

/**
 * Whether the reader may write here at all: the content's say and the comment section's, between
 * them. Each flag is true until its own payload says otherwise, so a payload still on its way is
 * not read as a refusal.
 */
fun CommentViewModel.CommentUiState.canCommentOn(content: ZhihuContent?): Boolean =
    canWrite && content?.commentConfig?.canReply != false
