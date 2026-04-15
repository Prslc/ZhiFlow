package com.prslc.zhiflow.ui.page.question

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.toApiException
import com.prslc.zhiflow.data.model.QuestionDetail
import com.prslc.zhiflow.data.model.QuestionFeedItem
import com.prslc.zhiflow.data.repository.QuestionRepository
import com.prslc.zhiflow.parser.QuestionParser
import com.prslc.zhiflow.parser.model.DetailElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

data class QuestionUiState(
    val isLoading: Boolean = false,
    val isNextLoading: Boolean = false,
    val question: QuestionDetail? = null,
    val elements: List<DetailElement> = emptyList(),
    val answers: List<QuestionFeedItem> = emptyList(),
    val error: ApiException? = null,
    val hasMore: Boolean = false
)

class QuestionViewModel(private val repository: QuestionRepository) : ViewModel() {

    var uiState by mutableStateOf(QuestionUiState())
        private set

    private var nextPageUrl: String? = null
    private var loadJob: Job? = null

    fun loadQuestion(id: String) {
        if (uiState.question != null) return

        loadJob?.cancel()
        uiState = QuestionUiState(isLoading = true)

        loadJob = viewModelScope.launch {
            val detailDeferred = async { repository.getQuestion(id) }
            val feedDeferred = async { repository.getQuestionFeed(id) }

            val detailResult = detailDeferred.await()
            val feedResult = feedDeferred.await()

            if (detailResult.isSuccess && feedResult.isSuccess) {
                val detailData = detailResult.getOrThrow()
                val feedResponse = feedResult.getOrThrow()

                val elements = withContext(Dispatchers.Default) {
                    QuestionParser.parse(detailData.detail)
                }

                nextPageUrl = feedResponse.paging.next

                uiState = uiState.copy(
                    isLoading = false,
                    question = detailData,
                    elements = elements,
                    answers = feedResponse.data,
                    hasMore = !feedResponse.paging.isEnd
                )
            } else {
                val error = (detailResult.exceptionOrNull() ?: feedResult.exceptionOrNull())
                uiState = uiState.copy(
                    isLoading = false,
                    error = error?.toApiException()
                )
            }
        }
    }

    fun loadMore(id: String) {
        val url = nextPageUrl
        if (uiState.isNextLoading || url == null || !uiState.hasMore) return

        uiState = uiState.copy(isNextLoading = true)

        viewModelScope.launch {
            repository.getQuestionFeed(id, nextUrl = url)
                .onSuccess { response ->
                    nextPageUrl = response.paging.next
                    uiState = uiState.copy(
                        isNextLoading = false,
                        answers = uiState.answers + response.data,
                        hasMore = !response.paging.isEnd
                    )
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    uiState = uiState.copy(isNextLoading = false)
                }
        }
    }
}