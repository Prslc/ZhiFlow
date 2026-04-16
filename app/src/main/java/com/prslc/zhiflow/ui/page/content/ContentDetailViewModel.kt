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

    private var loadJob: Job? = null
    private var parseJob: Job? = null

    val displayUpvoteCount: Int
        get() = (loadingState.content?.reaction?.statistics?.upVoteCount
            ?: 0) + interactionState.upvoteOffset

    /**
     * Load data by content type
     * @param id Content ID
     * @param type "answer" or "article"
     */
    fun loadContent(id: String, type: ContentType) {
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
            }.onFailure { e ->
                if (e is CancellationException) throw e
                loadingState = loadingState.copy(isLoading = false, error = e.toApiException())
            }
        }
    }

    fun parseRichText(isDark: Boolean) {
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
    fun updateVote(targetAction: String, contentType: ContentType) {
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


    fun updateReadProgress(contentToken: String, contentType: ContentType, progress: Int) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                actionRepository.syncHistory(
                    ReadHistoryRequest(contentToken, contentType.type, progress)
                )
            }
        }
    }

    fun updateFavoriteStatus(isFaved: Boolean) {
        interactionState = interactionState.copy(isFaved = isFaved)
    }

    private fun resetStates() {
        loadingState = LoadingState()
        interactionState = InteractionState()
        richTextElements = emptyList()
        parseJob?.cancel()
    }
}