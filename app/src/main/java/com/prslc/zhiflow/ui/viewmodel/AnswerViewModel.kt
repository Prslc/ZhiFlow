package com.prslc.zhiflow.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.exception.ApiException
import com.prslc.zhiflow.data.exception.toApiException
import com.prslc.zhiflow.data.model.ReadHistoryRequest
import com.prslc.zhiflow.data.model.ZhihuAnswer
import com.prslc.zhiflow.data.service.addReadHistory
import com.prslc.zhiflow.data.service.getAnswerDetail
import com.prslc.zhiflow.data.service.voteAction
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnswerViewModel : ViewModel() {
    var answer by mutableStateOf<ZhihuAnswer?>(null)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<ApiException?>(null)
    private var lastReportedProgress = -1

    var isUpvoted by mutableStateOf(false)
    var isDownvoted by mutableStateOf(false)
    var isFaved by mutableStateOf(false)
    private var upvoteOffset by mutableIntStateOf(0)

    val displayUpvoteCount: Int
        get() = (answer?.reaction?.statistics?.upVoteCount ?: 0) + upvoteOffset

    fun loadAnswer(answerId: String) {
        if (isLoading) return
        resetStates()
        isLoading = true

        viewModelScope.launch {
            try {
                val result = getAnswerDetail(answerId)
                answer = result

                result?.reaction?.relation?.let { rel ->
                    isUpvoted = (rel.vote == "UP")
                    isDownvoted = (rel.vote == "DOWN")
                    isFaved = (rel.faved)
                }

            } catch (e: Exception) {
                error = e.toApiException()
            } finally {
                isLoading = false
            }
        }
    }

    fun updateReadProgress(contentToken: String, contentType: String, progress: Int) {
        val tag = "addReadHistory"
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    val success =
                        addReadHistory(ReadHistoryRequest(contentToken, contentType, progress))
                    if (success) {
                        Log.d(tag, "Successfully synced progress: $progress%")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Final sync failed", e)
                }
            }
        }
    }

    private fun resetStates() {
        answer = null
        error = null
        isUpvoted = false
        isDownvoted = false
        upvoteOffset = 0
        lastReportedProgress = -1
    }

    fun updateVote(targetAction: String) {
        val id = answer?.id ?: return

        // check status
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
            voteAction(id, targetAction, method)
        }
    }
}