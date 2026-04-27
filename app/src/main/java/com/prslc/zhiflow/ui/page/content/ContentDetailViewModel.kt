package com.prslc.zhiflow.ui.page.content

import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.toApiException
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ReadHistoryRequest
import com.prslc.zhiflow.data.model.ZhihuContent
import com.prslc.zhiflow.data.repository.ActionRepository
import com.prslc.zhiflow.data.repository.ContentRepository
import com.prslc.zhiflow.parser.ContentParser
import com.prslc.zhiflow.parser.model.RichTextElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

sealed interface ContentEvent {
    data class Load(val id: String, val type: ContentType) : ContentEvent
    data class SetDarkMode(val isDark: Boolean) : ContentEvent
    data class Vote(val action: String, val contentType: ContentType) : ContentEvent
    data class ToggleFavorite(val isFaved: Boolean) : ContentEvent
    data class OpenLightbox(val index: Int) : ContentEvent
    data object DismissLightbox : ContentEvent
    data object OpenCollection : ContentEvent
    data object DismissCollection : ContentEvent
    data object OpenComments : ContentEvent
    data object DismissComments : ContentEvent
    data class TrackProgress(val progress: Int) : ContentEvent
    data class FlushProgress(val id: String, val type: ContentType) : ContentEvent
}

class ContentViewModel(
    private val repository: ContentRepository,
    private val actionRepository: ActionRepository
) : ViewModel() {

    companion object {
        private val parsingCache = LruCache<String, List<RichTextElement>>(20)
    }

    data class LoadingState(
        val isLoading: Boolean = false,
        val content: ZhihuContent? = null,
        val error: ApiException? = null
    )

    var loadingState by mutableStateOf(LoadingState())
        private set

    data class InteractionState(
        val isUpvoted: Boolean = false,
        val isDownvoted: Boolean = false,
        val isFaved: Boolean = false,
        val upvoteOffset: Int = 0
    )

    var interactionState by mutableStateOf(InteractionState())
        private set

    // 核心内容（解析后的元素列表）
    var richTextElements by mutableStateOf<List<RichTextElement>>(emptyList())
        private set

    data class PresentationState(
        val showCollectionSheet: Boolean = false,
        val showComments: Boolean = false,
        val isLightboxVisible: Boolean = false,
        val currentImageIndex: Int = 0
    )

    var presentation by mutableStateOf(PresentationState())
        private set

    private var readProgress by mutableStateOf(0)
    private var isDark by mutableStateOf(false)

    private var loadJob: Job? = null
    private var parseJob: Job? = null

    val displayUpvoteCount: Int
        get() = (loadingState.content?.reaction?.statistics?.upVoteCount
            ?: 0) + interactionState.upvoteOffset

    fun onEvent(event: ContentEvent) {
        when (event) {
            is ContentEvent.Load -> loadContent(event.id, event.type)
            is ContentEvent.SetDarkMode -> setDarkMode(event.isDark)
            is ContentEvent.Vote -> updateVote(event.action, event.contentType)
            is ContentEvent.ToggleFavorite -> interactionState = interactionState.copy(isFaved = event.isFaved)
            is ContentEvent.OpenLightbox -> presentation = presentation.copy(
                isLightboxVisible = true,
                currentImageIndex = event.index
            )
            is ContentEvent.DismissLightbox -> presentation = presentation.copy(isLightboxVisible = false)
            is ContentEvent.OpenCollection -> presentation = presentation.copy(showCollectionSheet = true)
            is ContentEvent.DismissCollection -> presentation = presentation.copy(showCollectionSheet = false)
            is ContentEvent.OpenComments -> presentation = presentation.copy(showComments = true)
            is ContentEvent.DismissComments -> presentation = presentation.copy(showComments = false)
            is ContentEvent.TrackProgress -> readProgress = event.progress
            is ContentEvent.FlushProgress -> sendReadProgress(event.id, event.type)
        }
    }

    /**
     * Load data by content type
     * @param id Content ID
     * @param type "answer" or "article"
     */
    private fun loadContent(id: String, type: ContentType) {
        loadJob?.cancel()
        resetStates()

        loadingState = LoadingState(isLoading = true)

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
                    isFaved = rel?.faved ?: false
                )
                parsingCache.get(data.id)?.let {
                    richTextElements = it
                }
                parseRichText()
            }.onFailure { e ->
                if (e is CancellationException) throw e
                loadingState = loadingState.copy(isLoading = false, error = e.toApiException())
            }
        }
    }

    private fun setDarkMode(dark: Boolean) {
        if (isDark == dark) return
        isDark = dark
        if (loadingState.content != null) parseRichText()
    }

    private fun parseRichText() {
        val content = loadingState.content ?: return
        val segments = content.structuredContent.segments

        if (richTextElements.isNotEmpty() && parsingCache.get(content.id) != null) return

        parseJob?.cancel()
        parseJob = viewModelScope.launch(Dispatchers.Default) {
            val fullList = mutableListOf<RichTextElement>()

            segments.chunked(10).forEachIndexed { _, chunk ->
                val chunkResult = ContentParser.transform(chunk, isDark)
                fullList.addAll(chunkResult)

                val currentSnapshot = fullList.toList()
                withContext(Dispatchers.Main) {
                    richTextElements = currentSnapshot
                }
            }
            parsingCache.put(content.id, fullList)
        }
    }

    // vote
    private fun updateVote(targetAction: String, contentType: ContentType) {
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
            upvoteOffset = newOffset
        )

        viewModelScope.launch {
            val isActive = if (targetAction == "up") was.isUpvoted else was.isDownvoted

            actionRepository.vote(
                id = contentId,
                type = contentType,
                action = targetAction,
                isRevoke = isActive
            ).onFailure { e ->
                if (e is CancellationException) throw e
                interactionState = was
            }
        }
    }

    private fun sendReadProgress(contentToken: String, contentType: ContentType) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                actionRepository.syncHistory(
                    ReadHistoryRequest(contentToken, contentType.type, readProgress)
                )
            }
        }
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
