package com.prslc.zhiflow.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.model.ZhihuAnswer
import com.prslc.zhiflow.data.model.ZhihuUser
import com.prslc.zhiflow.data.service.getAnswerDetail
import com.prslc.zhiflow.data.service.getUserDetail
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    var user by mutableStateOf<ZhihuUser?>(null)
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun loadProfile() {
        viewModelScope.launch {
            try {
                errorMessage = null
                isLoading = true

                val result = getUserDetail()

                if (result != null) {
                    user = result
                } else {
                    errorMessage = "Fetch User Profile failed"
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Unknown Error"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}