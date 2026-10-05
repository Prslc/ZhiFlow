package com.prslc.zhiflow.ui.page.feedback

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.data.dto.BlockTag
import com.prslc.zhiflow.data.dto.KeywordIssue
import com.prslc.zhiflow.data.repository.FeedbackRepository
import com.prslc.zhiflow.ui.navigation.FeedbackBlockList
import kotlinx.coroutines.launch

class FeedbackBlockListViewModel(
    private val repository: FeedbackRepository,
) : ViewModel() {

    @Immutable
    data class BlockUiState(
        val tags: List<BlockTag> = emptyList(),
        val keywords: List<String> = emptyList(),
        val minLength: Int = 0,
        val maxLength: Int = 0,
        val maxCount: Int = 0,
        val input: String = "",
        val keywordIssue: KeywordIssue? = null,
        val isLoading: Boolean = false,
        val loadError: ApiException? = null,
        val actionError: ApiException? = null,
        val isSubmitting: Boolean = false,
        val isSubmitted: Boolean = false,
    )

    var uiState by mutableStateOf(BlockUiState())
        private set

    private var route: FeedbackBlockList? = null

    fun load(route: FeedbackBlockList) {
        this.route = route
        uiState = BlockUiState(isLoading = true)
        fetch(route)
    }

    fun retry() {
        val route = route ?: return
        uiState = uiState.copy(isLoading = true, loadError = null)
        fetch(route)
    }

    private fun fetch(route: FeedbackBlockList) {
        viewModelScope.launch {
            repository.getBlockOptions(route.contentToken, route.contentType, route.feedbackType)
                .onSuccess { options ->
                    uiState = uiState.copy(
                        tags = options.tags,
                        keywords = options.keywords,
                        minLength = options.keywordMinLength,
                        maxLength = options.keywordMaxLength,
                        maxCount = options.keywordMaxCount,
                        isLoading = false,
                    )
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(loadError = error, isLoading = false)
                }
        }
    }

    fun toggleTag(id: Long) {
        uiState = uiState.copy(
            tags = uiState.tags.map { tag ->
                if (tag.id == id) tag.copy(isSelected = !tag.isSelected) else tag
            },
        )
    }

    fun onInputChange(value: String) {
        uiState = uiState.copy(input = value, keywordIssue = null)
    }

    fun addKeyword() {
        val keyword = uiState.input.trim()
        val issue = issueWith(keyword)
        if (issue != null) {
            uiState = uiState.copy(keywordIssue = issue)
            return
        }

        uiState = uiState.copy(keywords = uiState.keywords + keyword, input = "", keywordIssue = null)
    }

    fun removeKeyword(keyword: String) {
        uiState = uiState.copy(keywords = uiState.keywords - keyword, keywordIssue = null)
    }

    fun submit() {
        val route = route ?: return
        if (uiState.isSubmitting) return

        val tags = uiState.tags
        val keywords = uiState.keywords
        uiState = uiState.copy(isSubmitting = true)

        viewModelScope.launch {
            repository.submitBlockFeedback(route.contentToken, route.contentType, tags, keywords)
                .onSuccess { uiState = uiState.copy(isSubmitting = false, isSubmitted = true) }
                .onApiFailure { error ->
                    uiState = uiState.copy(isSubmitting = false, actionError = error)
                }
        }
    }

    fun consumeActionError() {
        uiState = uiState.copy(actionError = null)
    }

    private fun issueWith(keyword: String): KeywordIssue? {
        val state = uiState
        return when {
            keyword.isEmpty() || keyword.length < state.minLength -> KeywordIssue.TOO_SHORT
            keyword.length > state.maxLength -> KeywordIssue.TOO_LONG
            state.keywords.size >= state.maxCount -> KeywordIssue.MAX_COUNT
            state.keywords.any { it.equals(keyword, ignoreCase = true) } -> KeywordIssue.DUPLICATE
            else -> null
        }
    }
}
