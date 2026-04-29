package com.prslc.zhiflow.ui.page.profile

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.toApiException
import com.prslc.zhiflow.data.model.ZhihuUser
import com.prslc.zhiflow.data.repository.UserRepository
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class ProfileViewModel(private val repository: UserRepository) : ViewModel() {

    @Stable
    data class UserUiState(
        val isLoading: Boolean = false,
        val user: ZhihuUser? = null,
        val error: ApiException? = null
    )

    var uiState by mutableStateOf(UserUiState())
        private set

    fun loadProfile() {
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            repository.getMyDetail()
                .onSuccess { user ->
                    uiState = uiState.copy(user = user, isLoading = false)
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    uiState = uiState.copy(error = e.toApiException(), isLoading = false)
                }
        }
    }
}
