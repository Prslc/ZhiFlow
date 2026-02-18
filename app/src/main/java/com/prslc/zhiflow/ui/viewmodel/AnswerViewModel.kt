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

    fun loadAnswer(answerId: String) {
        if (isLoading) return
        answer = null
        error = null
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
}