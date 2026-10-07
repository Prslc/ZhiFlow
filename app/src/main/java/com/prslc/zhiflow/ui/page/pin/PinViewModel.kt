package com.prslc.zhiflow.ui.page.pin

import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.ignoreOutcome
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.model.content.ZhihuImage
import com.prslc.zhiflow.data.model.content.ZhihuPin
import com.prslc.zhiflow.data.model.user.ReadHistoryRequest
import com.prslc.zhiflow.data.repository.ActionRepository
import com.prslc.zhiflow.data.repository.ContentRepository
import com.prslc.zhiflow.data.repository.UserRepository
import com.prslc.zhiflow.data.remote.parser.ContentParser
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PinViewModel(
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
        val content: ZhihuPin? = null,
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

    @Immutable
    data class PresentationState(
        val showCollectionSheet: Boolean = false,
        val showComments: Boolean = false,
    )

    var presentation by mutableStateOf(PresentationState())
        private set

    private var readProgress by mutableIntStateOf(0)
    private var isDark by mutableStateOf(false)

    private var loadJob: Job? = null
    private var parseJob: Job? = null

    /** Null until the content has loaded; the bottom bar renders no count while null. */
    val displayUpvoteCount: Int?
        get() = loadingState.content?.let { content ->
            content.reaction.statistics.upVoteCount + interactionState.upvoteOffset
        }

    fun loadContent(id: String) {
        loadJob?.cancel()
        resetStates()
        loadJob = viewModelScope.launch {
            repository.getPin(id)
                .onSuccess { data ->
                    val rel = data.reaction.relation
                    loadingState = LoadingState(content = data)
                    interactionState = InteractionState(
                        isUpvoted = rel?.vote == "UP",
                        isDownvoted = rel?.vote == "DOWN",
                        isFavorite = rel?.faved ?: false,
                    )
                    parsingCache.get(data.id)?.let {
                        richTextElements = it
                    }
                    parseRichText()
                }
                .onApiFailure { error ->
                    loadingState = loadingState.copy(error = error)
                }
        }
    }

    fun setDarkMode(dark: Boolean) {
        if (isDark == dark) return
        isDark = dark
        if (loadingState.content != null) parseRichText()
    }

    fun vote(targetAction: String) {
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
                type = ContentType.PIN,
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

    fun trackProgress(progress: Int) {
        readProgress = progress
    }

    fun flushProgress(contentToken: String) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                actionRepository.syncHistory(
                    ReadHistoryRequest(contentToken, ContentType.PIN.type, readProgress)
                ).ignoreOutcome()
            }
        }
    }

    private fun parseRichText() {
        val content = loadingState.content ?: return

        if (richTextElements.isNotEmpty() && parsingCache.get(content.id) != null) return

        parseJob?.cancel()
        parseJob = viewModelScope.launch(Dispatchers.Default) {
            val fullList = mutableListOf<RichTextElement>()
            val segments = content.structuredContent?.segments.orEmpty()

            if (segments.isEmpty()) {
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
            } else {
                segments.chunked(10).forEachIndexed { _, chunk ->
                    val chunkResult = ContentParser.transform(chunk, isDark)
                    fullList.addAll(chunkResult)

                    val currentSnapshot = fullList.toList()
                    withContext(Dispatchers.Main) {
                        richTextElements = currentSnapshot
                    }
                }
            }

            val finalSnapshot = fullList.toList()
            withContext(Dispatchers.Main) {
                richTextElements = finalSnapshot
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
     * Optimistically toggles the follow state of the pin author,
     * rolling back on API failure.
     */
    fun toggleFollow() {
        val content = loadingState.content ?: return
        val author = content.author
        if (author.id.isEmpty()) return
        if (followJob?.isActive == true) return

        val following = author.followStatus == FOLLOWING
        val target = if (following) UNFOLLOWED else FOLLOWING
        loadingState = loadingState.copy(content = content.copy(author = author.copy(followStatus = target)))

        followJob = viewModelScope.launch {
            val result = if (following) userRepository.unfollowUser(author.id)
            else userRepository.followUser(author.id)
            result.onApiFailure { error ->
                rollback(following)
                actionError = error
            }
        }
    }

    private fun rollback(previousFollowing: Boolean) {
        val content = loadingState.content ?: return
        val status = if (previousFollowing) FOLLOWING else UNFOLLOWED
        loadingState = loadingState.copy(content = content.copy(author = content.author.copy(followStatus = status)))
    }

    private fun resetStates() {
        loadingState = LoadingState()
        interactionState = InteractionState()
        richTextElements = emptyList()
        presentation = PresentationState()
        readProgress = 0
        parseJob?.cancel()
    }
}
