package com.prslc.zhiflow.ui.page.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.toApiException
import com.prslc.zhiflow.data.model.ZhihuUser
import com.prslc.zhiflow.data.service.getUserDetail
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    var user by mutableStateOf<ZhihuUser?>(null)
    var isLoading by mutableStateOf(false)

    var error by mutableStateOf<ApiException?>(null)

    fun loadProfile() {
        viewModelScope.launch {
            try {
                error = null
                isLoading = true

                val result = getUserDetail()

                if (result != null) {
                    user = result
                } else {
                    error = ApiException.UnknownException()
                }
            } catch (e: Exception) {
                error = e.toApiException()
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }
}