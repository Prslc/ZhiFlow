package com.prslc.zhiflow.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.model.ZhihuAnswer
import com.prslc.zhiflow.data.service.getAnswerDetail
import kotlinx.coroutines.launch

class AnswerViewModel : ViewModel() {

    var answer by mutableStateOf<ZhihuAnswer?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun loadAnswer(answerId: String) {
        if (isLoading) return

        answer = null
        errorMessage = null
        isLoading = true

        viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = null
                val result = getAnswerDetail(answerId)
                if (result != null) {
                    answer = result
                } else {
                    errorMessage = "empty"
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "unknown error"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}