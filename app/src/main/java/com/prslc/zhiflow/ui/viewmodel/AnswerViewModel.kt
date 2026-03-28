package com.prslc.zhiflow.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.exception.ApiException
import com.prslc.zhiflow.data.exception.toApiException
import com.prslc.zhiflow.data.model.ZhihuAnswer
import com.prslc.zhiflow.data.service.getAnswerDetail
import kotlinx.coroutines.launch

class AnswerViewModel : ViewModel() {
    var answer by mutableStateOf<ZhihuAnswer?>(null)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<ApiException?>(null)

    var isUpvoted by mutableStateOf(false)
    var isDownvoted by mutableStateOf(false)
    private var upvoteOffset by mutableStateOf(0)

    val displayUpvoteCount: Int
        get() = (answer?.reaction?.statistics?.upVoteCount ?: 0) + upvoteOffset

    fun loadAnswer(answerId: String) {
        if (isLoading) return
        resetStates()
        isLoading = true

        viewModelScope.launch {
            try {
                answer = getAnswerDetail(answerId)
            } catch (e: Exception) {
                error = e.toApiException()
            } finally {
                isLoading = false
            }
        }
    }

    private fun resetStates() {
        answer = null
        error = null
        isUpvoted = false
        isDownvoted = false
        upvoteOffset = 0
    }

    // send like
    fun toggleUpvote() {
        val previousUpvoted = isUpvoted
        val previousOffset = upvoteOffset

        if (isUpvoted) {
            isUpvoted = false
            upvoteOffset -= 1
        } else {
            isUpvoted = true
            upvoteOffset += 1
            if (isDownvoted) isDownvoted = false
        }

        // TODO
    }

    fun toggleDownvote() {
        isDownvoted = !isDownvoted
        if (isDownvoted && isUpvoted) {
            toggleUpvote()
        }
    }
}