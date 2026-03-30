package com.prslc.zhiflow.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.exception.ApiException
import com.prslc.zhiflow.data.exception.toApiException
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ReadHistoryRequest
import com.prslc.zhiflow.data.model.ZhihuContent
import com.prslc.zhiflow.data.service.addReadHistory
import com.prslc.zhiflow.data.service.getAnswerDetail
import com.prslc.zhiflow.data.service.getArticleDetail
import com.prslc.zhiflow.data.service.voteAction
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContentViewModel : ViewModel() {
    var content by mutableStateOf<ZhihuContent?>(null)

    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<ApiException?>(null)

    var isUpvoted by mutableStateOf(false)
    var isDownvoted by mutableStateOf(false)
    var isFaved by mutableStateOf(false)
    private var upvoteOffset by mutableIntStateOf(0)

    val displayUpvoteCount: Int
        get() = (content?.reaction?.statistics?.upVoteCount ?: 0) + upvoteOffset

    /**
     * Load data by content type
     * @param id Content ID
     * @param type "answer" or "article"
     */
    fun loadContent(id: String, type: ContentType) {
        if (isLoading) return
        resetStates()
        isLoading = true

        viewModelScope.launch {
            try {
                val result: ZhihuContent? = when (type) {
                    ContentType.ARTICLE -> getArticleDetail(id)
                    ContentType.ANSWER -> getAnswerDetail(id)
                }

                content = result

                result?.reaction?.relation?.let { rel ->
                    isUpvoted = (rel.vote == "UP")
                    isDownvoted = (rel.vote == "DOWN")
                    isFaved = rel.faved
                }
            } catch (e: Exception) {
                error = e.toApiException()
            } finally {
                isLoading = false
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
                    throw  e
                }
            }
        }
    }

    private fun resetStates() {
        content = null
        error = null
        isUpvoted = false
        isDownvoted = false
        upvoteOffset = 0
    }
}