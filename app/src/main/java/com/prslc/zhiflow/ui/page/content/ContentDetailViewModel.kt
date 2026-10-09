package com.prslc.zhiflow.ui.page.content

import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.ignoreOutcome
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.core.utils.compose.ReadingPosition
import com.prslc.zhiflow.core.utils.compose.ReadingProgress
import com.prslc.zhiflow.data.model.content.AnswerAuthor
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.data.model.content.ZhihuAnswer
import com.prslc.zhiflow.data.model.content.ZhihuArticle
import com.prslc.zhiflow.data.model.content.ZhihuContent
import com.prslc.zhiflow.data.model.content.ZhihuImage
import com.prslc.zhiflow.data.model.content.ZhihuPin
import com.prslc.zhiflow.data.model.user.ReadHistoryRequest
import com.prslc.zhiflow.data.remote.parser.ContentParser
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.data.repository.ActionRepository
import com.prslc.zhiflow.data.repository.ContentRepository
import com.prslc.zhiflow.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContentViewModel(
    private val repository: ContentRepository,
    private val actionRepository: ActionRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private val parsingCache = LruCache<String, List<RichTextElement>>(20)
        private const val FOLLOWING = "following"
        private const val UNFOLLOWED = "normal"
    }

    @Stable
    data class LoadingState(
        val content: ZhihuContent? = null,
        val error: ApiException? = null,
    )

    var loadingState by mutableStateOf(LoadingState())
        private set

    @Immutable
    data class InteractionState(
        val isUpvoted: Boolean = false,
        val isDownvoted: Boolean = false,
        val isFavorite: Boolean = false,
        val upvoteOffset: Int = 0,
    )

    var interactionState by mutableStateOf(InteractionState())
        private set

    var richTextElements by mutableStateOf<List<RichTextElement>>(emptyList())
        private set

    /**
     * Live state of the body's `seg_like` ranges, keyed by [SegmentLikeTarget.key].
     *
     * Seeded from the parsed paragraphs — the parsed text only carries the range, never the state —
     * and kept outside [parsingCache] so a like never has to invalidate a parsed body.
     */
    var segmentLikes by mutableStateOf<Map<String, SegmentLikeTarget>>(emptyMap())
        private set

    /** Key of the range whose panel is open; null when it is closed. */
    var openSegmentKey by mutableStateOf<String?>(null)
        private set

    @Immutable
    data class PresentationState(
        val showCollectionSheet: Boolean = false,
        val showComments: Boolean = false,
    )

    var presentation by mutableStateOf(PresentationState())
        private set

    private val progress = ReadingProgress()

    /** Reading position as a 0f..1f fraction: the progress bar draws it, [flushProgress] reports it. */
    val readProgress: Float
        get() = progress.fraction

    /** True once [richTextElements] holds the whole body; the list measures nothing before that. */
    var isBodyComplete by mutableStateOf(false)
        private set


    private var contentType: ContentType? = null
    private val pendingSegmentLikes = mutableSetOf<String>()

    private var loadJob: Job? = null
    private var parseJob: Job? = null

    /** Null until the content has loaded; the bottom bar renders no count while null. */
    val displayUpvoteCount: Int?
        get() = loadingState.content?.let { content ->
            (content.reaction?.statistics?.upVoteCount ?: 0) + interactionState.upvoteOffset
        }

    /**
     * Load content by [ContentType]
     *
     * @param id Content ID
     * @param type Content type (ARTICLE, ANSWER, or PIN)
     */
    fun loadContent(id: String, type: ContentType) {
        loadJob?.cancel()
        contentType = type
        resetStates()
        loadJob = viewModelScope.launch {
            val result = when (type) {
                ContentType.ARTICLE -> repository.getArticle(id)
                ContentType.ANSWER -> repository.getAnswer(id)
                ContentType.PIN -> repository.getPin(id)
            }

            result.onSuccess { data ->
                val rel = data.reaction?.relation
                loadingState = LoadingState(content = data)
                interactionState = InteractionState(
                    isUpvoted = rel?.vote == "UP",
                    isDownvoted = rel?.vote == "DOWN",
                    isFavorite = rel?.faved ?: false,
                )
                parsingCache.get(data.id)?.let {
                    setElements(it)
                    isBodyComplete = true
                }
                parseRichText()
            }.onApiFailure { error ->
                loadingState = loadingState.copy(error = error)
            }
        }
    }

    /**
     * Vote or revoke a vote with optimistic UI.
     *
     * Applies the vote state immediately and rolls back on API failure.
     */
    fun vote(targetAction: String, contentType: ContentType) {
        val currentContent = loadingState.content ?: return
        val contentId = currentContent.id

        val was = interactionState

        var newUpvoted = was.isUpvoted
        var newDownvoted = was.isDownvoted
        var newOffset = was.upvoteOffset

        when (targetAction) {
            "up" -> {
                newUpvoted = !was.isUpvoted
                newOffset += if (newUpvoted) 1 else -1
                if (newUpvoted) newDownvoted = false
            }

            "down" -> {
                newDownvoted = !was.isDownvoted
                if (newDownvoted && was.isUpvoted) {
                    newUpvoted = false
                    newOffset--
                }
            }
        }

        interactionState = was.copy(
            isUpvoted = newUpvoted,
            isDownvoted = newDownvoted,
            upvoteOffset = newOffset,
        )

        viewModelScope.launch {
            val isActive = if (targetAction == "up") was.isUpvoted else was.isDownvoted

            actionRepository.vote(
                id = contentId,
                type = contentType,
                action = targetAction,
                isRevoke = isActive
            ).onApiFailure { error ->
                interactionState = was
                actionError = error
            }
        }
    }

    fun setFaved(isFavorite: Boolean) {
        interactionState = interactionState.copy(isFavorite = isFavorite)
    }

    fun openCollection() {
        presentation = presentation.copy(showCollectionSheet = true)
    }

    fun dismissCollection() {
        presentation = presentation.copy(showCollectionSheet = false)
    }

    fun openComments() {
        presentation = presentation.copy(showComments = true)
    }

    fun dismissComments() {
        presentation = presentation.copy(showComments = false)
    }

    fun openSegmentPanel(key: String) {
        openSegmentKey = key
    }

    fun dismissSegmentPanel() {
        openSegmentKey = null
    }

    /**
     * Toggles the like on one passage with optimistic UI, rolling back on failure.
     *
     * The like request answers with the reader's own segment id, and that is the one undoing needs
     * — the shared id the page was parsed with is not accepted for it. So the answer is folded back
     * into the state, and the next tap on this range can undo.
     */
    fun toggleSegmentLike(key: String) {
        val content = loadingState.content ?: return
        val type = contentType ?: return
        val target = segmentLikes[key] ?: return
        if (!pendingSegmentLikes.add(key)) return

        val shouldLike = !target.isLiked
        updateSegmentLike(key) {
            it.copy(
                isLiked = shouldLike,
                likeCount = (it.likeCount + if (shouldLike) 1 else -1).coerceAtLeast(0),
            )
        }

        viewModelScope.launch {
            actionRepository.toggleSegmentLike(
                id = content.id,
                type = type,
                target = target,
                isLike = shouldLike,
            ).onSuccess { mySegId ->
                if (shouldLike && !mySegId.isNullOrEmpty()) {
                    updateSegmentLike(key) { it.copy(mySegId = mySegId) }
                }
            }.onApiFailure { error ->
                updateSegmentLike(key) { target }
                actionError = error
            }
            pendingSegmentLikes.remove(key)
        }
    }

    private fun updateSegmentLike(
        key: String,
        transform: (SegmentLikeTarget) -> SegmentLikeTarget,
    ) {
        val current = segmentLikes[key] ?: return
        segmentLikes = segmentLikes + (key to transform(current))
    }

    /**
     * Publishes a freshly parsed body and lifts its `seg_like` ranges into [segmentLikes].
     *
     * Ranges arrive a chunk at a time, so the state only ever grows; a range already in it keeps
     * the live like the reader just made.
     */
    private fun setElements(elements: List<RichTextElement>) {
        richTextElements = elements

        val targets = elements
            .filterIsInstance<RichTextElement.ParsedText>()
            .flatMap { it.segmentLikes }
            .filterNot { segmentLikes.containsKey(it.key) }

        if (targets.isNotEmpty()) {
            segmentLikes = segmentLikes + targets.associateBy { it.key }
        }
    }

    /** Records where the reader is; the list only ever sends a position it could measure. */
    fun trackProgress(position: ReadingPosition) {
        progress.update(position)
    }

    /**
     * Flush reading progress to the server.
     *
     * Runs on [NonCancellable] because it fires as the screen goes away, and the request has to
     * outlive the composition that triggered it.
     */
    fun flushProgress(contentToken: String, contentType: ContentType) {
        val percent = progress.reportedPercent()
        // 0 means nothing was read.
        if (percent <= 0) return

        viewModelScope.launch {
            withContext(NonCancellable) {
                actionRepository.syncHistory(
                    ReadHistoryRequest(contentToken, contentType.type, percent)
                ).ignoreOutcome()
            }
        }
    }

    private fun parseRichText() {
        val content = loadingState.content ?: return
        val segments = content.structuredContent?.segments.orEmpty()

        if (richTextElements.isNotEmpty() && parsingCache.get(content.id) != null) return

        isBodyComplete = false
        parseJob?.cancel()
        parseJob = viewModelScope.launch(Dispatchers.Default) {
            val fullList = mutableListOf<RichTextElement>()

            if (segments.isEmpty() && content is ZhihuPin) {
                content.imageList?.images?.forEach { pinImage ->
                    pinImage.url?.let { url ->
                        fullList.add(
                            RichTextElement.Image(
                                ZhihuImage(
                                    urls = listOf(url),
                                    width = pinImage.width,
                                    height = pinImage.height,
                                    description = "",
                                    isGif = false,
                                )
                            )
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    setElements(fullList.toList())
                    isBodyComplete = true
                }
                parsingCache.put(content.id, fullList)
                return@launch
            }

            segments.chunked(10).forEachIndexed { _, chunk ->
                val chunkResult = ContentParser.transform(chunk)
                fullList.addAll(chunkResult)

                val currentSnapshot = fullList.toList()
                withContext(Dispatchers.Main) {
                    setElements(currentSnapshot)
                }
            }
            // The body is complete from here, which is what makes its length a usable scale.
            withContext(Dispatchers.Main) {
                isBodyComplete = true
            }
            parsingCache.put(content.id, fullList)
        }
    }

    private var followJob: Job? = null

    /** Set when a follow toggle fails, so the screen can explain the rollback. */
    var actionError by mutableStateOf<ApiException?>(null)
        private set

    fun consumeActionError() {
        actionError = null
    }

    /**
     * Optimistically toggles the follow state of the content author,
     * rolling back on API failure.
     */
    fun toggleFollow() {
        val author = loadingState.content?.author ?: return
        if (author.id.isEmpty()) return
        if (followJob?.isActive == true) return

        val following = author.followStatus == FOLLOWING
        val target = if (following) UNFOLLOWED else FOLLOWING
        updateAuthor { it.copy(followStatus = target) }

        followJob = viewModelScope.launch {
            val result = if (following) userRepository.unfollowUser(author.id)
            else userRepository.followUser(author.id)
            result.onApiFailure { error ->
                updateAuthor { it.copy(followStatus = if (following) FOLLOWING else UNFOLLOWED) }
                actionError = error
            }
        }
    }

    private fun updateAuthor(transform: (AnswerAuthor) -> AnswerAuthor) {
        val content = loadingState.content ?: return
        val updated = when (content) {
            is ZhihuAnswer -> content.copy(author = transform(content.author))
            is ZhihuArticle -> content.copy(author = transform(content.author))
            is ZhihuPin -> content.copy(author = transform(content.author))
            else -> content
        }
        loadingState = loadingState.copy(content = updated)
    }

    private fun resetStates() {
        loadingState = LoadingState()
        interactionState = InteractionState()
        richTextElements = emptyList()
        segmentLikes = emptyMap()
        openSegmentKey = null
        pendingSegmentLikes.clear()
        presentation = PresentationState()
        progress.reset()
        isBodyComplete = false
        parseJob?.cancel()
    }
}
