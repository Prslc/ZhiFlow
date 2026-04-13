package com.prslc.zhiflow.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.exception.toApiException
import com.prslc.zhiflow.data.model.QuestionDetail
import com.prslc.zhiflow.data.model.QuestionFeedItem
import com.prslc.zhiflow.data.service.getQuestionDetail
import com.prslc.zhiflow.data.service.getQuestionFeed
import com.prslc.zhiflow.parser.QuestionParser
import com.prslc.zhiflow.parser.model.DetailElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

sealed interface QuestionUiState {
    data object Loading : QuestionUiState
    data class Error(val exception: Throwable) : QuestionUiState
    data class Success(
        val question: QuestionDetail,
        val elements: List<DetailElement>,
        val answers: List<QuestionFeedItem> = emptyList()
    ) : QuestionUiState
}

class QuestionViewModel : ViewModel() {
    private var loadJob: Job? = null
    var uiState by mutableStateOf<QuestionUiState>(QuestionUiState.Loading)
        private set
    var answerItems = mutableStateListOf<QuestionFeedItem>()
        private set

    var isNextLoading by mutableStateOf(false)
    private var nextPageUrl: String? = null

    fun loadQuestion(id: String) {
        if (uiState is QuestionUiState.Success) return

        loadJob?.cancel()
        uiState = QuestionUiState.Loading

        loadJob = viewModelScope.launch {
            try {
                val detailDeferred = async { getQuestionDetail(id) }
                val feedDeferred = async { getQuestionFeed(id) }

                val detailData = detailDeferred.await()
                val feedResponse = feedDeferred.await()

                val elements = withContext(Dispatchers.Default) {
                    QuestionParser.parse(detailData.detail)
                }

                answerItems.clear()
                answerItems.addAll(feedResponse.data)
                nextPageUrl = feedResponse.paging.next

                uiState = QuestionUiState.Success(
                    question = detailData,
                    elements = elements
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                uiState = QuestionUiState.Error(e.toApiException())
            }
        }
    }

    fun loadMore(id: String) {
        val url = nextPageUrl
        if (isNextLoading || url == null) return

        viewModelScope.launch {
            try {
                isNextLoading = true
                val response = getQuestionFeed(id, nextUrl = url)
                answerItems.addAll(response.data)
                nextPageUrl = response.paging.next
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                isNextLoading = false
            }
        }
    }
}