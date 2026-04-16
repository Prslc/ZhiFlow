package com.prslc.zhiflow.ui.page.content

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

    data class ContentUiState(
        val isLoading: Boolean = false,
        val content: ZhihuContent? = null,
        val richTextElements: List<RichTextElement> = emptyList(),
        val error: ApiException? = null,
        val isUpvoted: Boolean = false,
        val isDownvoted: Boolean = false,
        val isFaved: Boolean = false,
        val upvoteOffset: Int = 0
    )

    var uiState by mutableStateOf(ContentUiState())
        private set

    private var loadJob: Job? = null

    val displayUpvoteCount: Int
        get() = (uiState.content?.reaction?.statistics?.upVoteCount ?: 0) + uiState.upvoteOffset

    /**
     * Load data by content type
     * @param id Content ID
     * @param type "answer" or "article"
     */
    fun loadContent(id: String, type: ContentType) {
        loadJob?.cancel()

        resetStates()
        uiState = ContentUiState(isLoading = true)

        loadJob = viewModelScope.launch {
            val result: Result<ZhihuContent> = when (type) {
                ContentType.ARTICLE -> repository.getArticle(id)
                ContentType.ANSWER -> repository.getAnswer(id)
                ContentType.PIN -> repository.getPin(id)
            }

            result.onSuccess { data: ZhihuContent ->
                val rel = data.reaction?.relation
                uiState = uiState.copy(
                    isLoading = false,
                    content = data,
                    isUpvoted = rel?.vote == "UP",
                    isDownvoted = rel?.vote == "DOWN",
                    isFaved = rel?.faved ?: false
                )
            }.onFailure { e ->
                if (e is CancellationException) throw e
                uiState = uiState.copy(
                    isLoading = false,
                    error = e.toApiException()
                )
            }
        }
    }

    fun parseRichText(
        isDark: Boolean
    ) {
        val currentContent = uiState.content ?: return
        val segments = currentContent.structuredContent.segments

        viewModelScope.launch(Dispatchers.Default) {
            val result = withContext(Dispatchers.Default) {
                ContentParser.transform(
                    segments = segments,
                    isDark = isDark
                )
            }
            withContext(Dispatchers.Main) {
                uiState = uiState.copy(richTextElements = result)
            }
        }
    }


    // vote
    fun updateVote(targetAction: String, contentType: ContentType) {
        val currentContent = uiState.content ?: return
        val id = currentContent.id
        
        val wasUpvoted = uiState.isUpvoted
        val wasDownvoted = uiState.isDownvoted
        val wasOffset = uiState.upvoteOffset

        var newUpvoted = wasUpvoted
        var newDownvoted = wasDownvoted
        var newOffset = wasOffset

        when (targetAction) {
            "up" -> {
                newUpvoted = !wasUpvoted
                newOffset += if (newUpvoted) 1 else -1
                if (newUpvoted) newDownvoted = false
            }
            "down" -> {
                newDownvoted = !wasDownvoted
                if (newDownvoted && wasUpvoted) {
                    newUpvoted = false
                    newOffset--
                }
            }
        }

        uiState = uiState.copy(
            isUpvoted = newUpvoted,
            isDownvoted = newDownvoted,
            upvoteOffset = newOffset
        )

        viewModelScope.launch {
            val isActive = if (targetAction == "up") wasUpvoted else wasDownvoted

            actionRepository.vote(
                id = id,
                type = contentType,
                action = targetAction,
                isRevoke = isActive
            ).onFailure { e ->
                if (e is CancellationException) throw e
                uiState = uiState.copy(
                    isUpvoted = wasUpvoted,
                    isDownvoted = wasDownvoted,
                    upvoteOffset = wasOffset
                )
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
        uiState = uiState.copy(isFaved = isFaved)
    }

    private fun resetStates() {
        uiState = ContentUiState()
    }
}