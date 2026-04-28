package com.prslc.zhiflow.ui.page.people

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

class PeopleViewModel(private val repository: UserRepository) : ViewModel() {

    data class PeopleUiState(
        val isLoading: Boolean = false,
        val user: ZhihuUser? = null,
        val error: ApiException? = null
    )

    var uiState by mutableStateOf(PeopleUiState(isLoading = true))
        private set

    fun loadPeople(urlToken: String) {
        uiState = PeopleUiState(isLoading = true)

        viewModelScope.launch {
            repository.getUserDetail(urlToken)
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
