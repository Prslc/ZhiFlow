package com.prslc.zhiflow.ui.page.content

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.latex.renderer.model.LatexConfig
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.toApiException
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ReadHistoryRequest
import com.prslc.zhiflow.data.model.ZhihuContent
import com.prslc.zhiflow.data.service.addReadHistory
import com.prslc.zhiflow.data.service.getAnswerDetail
import com.prslc.zhiflow.data.service.getArticleDetail
import com.prslc.zhiflow.data.service.getPinDetail
import com.prslc.zhiflow.data.service.voteAction
import com.prslc.zhiflow.parser.ContentParser
import com.prslc.zhiflow.parser.model.RichTextElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class ContentViewModel : ViewModel() {
    private var loadJob: Job? = null

    var content by mutableStateOf<ZhihuContent?>(null)

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<ApiException?>(null)

    var isUpvoted by mutableStateOf(false)
    var isDownvoted by mutableStateOf(false)
    var isFaved by mutableStateOf(false)
    private var upvoteOffset by mutableIntStateOf(0)

    var richTextElements by mutableStateOf<List<RichTextElement>>(emptyList())
        private set

    val displayUpvoteCount: Int
        get() = (content?.reaction?.statistics?.upVoteCount ?: 0) + upvoteOffset

    /**
     * Load data by content type
     * @param id Content ID
     * @param type "answer" or "article"
     */
    fun loadContent(id: String, type: ContentType) {
        loadJob?.cancel()

        resetStates()
        isLoading = true

        loadJob = viewModelScope.launch {
            val result = runCatching {
                when (type) {
                    ContentType.ARTICLE -> getArticleDetail(id)
                    ContentType.ANSWER -> getAnswerDetail(id)
                    ContentType.PIN -> getPinDetail(id)
                }
            }

            result.onSuccess { data ->
                content = data
                data?.reaction?.relation?.let { rel ->
                    isUpvoted = (rel.vote == "UP")
                    isDownvoted = (rel.vote == "DOWN")
                    isFaved = rel.faved
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                error = e.toApiException()
            }
            // finally
            isLoading = false
        }
    }

    fun parseRichText(
        measurer: LatexMeasurerState,
        density: Density,
        config: LatexConfig,
        isDark: Boolean
    ) {
        val currentContent = this.content ?: return
        val segments = currentContent.structuredContent.segments

        viewModelScope.launch(Dispatchers.Default) {
            val result = ContentParser.transform(
                segments = segments,
                measurer = measurer,
                density = density,
                config = config,
                isDark = isDark
            )
            withContext(Dispatchers.Main) {
                richTextElements = result
            }
        }
    }


    // vote
    fun updateVote(targetAction: String, contentType: ContentType) {
        val id = content?.id ?: return

        val isActive = if (targetAction == "up") isUpvoted else isDownvoted
        val method = if (isActive) "DELETE" else "POST"

        when (targetAction) {
            "up" -> {
                isUpvoted = !isUpvoted
                upvoteOffset += if (isUpvoted) 1 else -1
                if (isUpvoted) isDownvoted = false
            }

            "down" -> {
                isDownvoted = !isDownvoted
                if (isDownvoted && isUpvoted) {
                    isUpvoted = false
                    upvoteOffset--
                }
            }
        }

        viewModelScope.launch {
            voteAction(id, contentType, targetAction, method)
        }
    }


    fun updateReadProgress(contentToken: String, contentType: ContentType, progress: Int) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    addReadHistory(ReadHistoryRequest(contentToken, contentType.type, progress))
                } catch (e: Exception) {

                }
            }
        }
    }

    private fun resetStates() {
        content = null
        richTextElements = emptyList()
        error = null
        isUpvoted = false
        isDownvoted = false
        upvoteOffset = 0
    }
}