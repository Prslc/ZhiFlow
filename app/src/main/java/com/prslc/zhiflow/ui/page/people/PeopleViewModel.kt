package com.prslc.zhiflow.ui.page.people

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.data.model.ZhihuUser
import com.prslc.zhiflow.data.repository.UserRepository
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class PeopleViewModel(private val repository: UserRepository) : ViewModel() {

    @Immutable
    data class PeopleUiState(
        val isLoading: Boolean = false,
        val user: ZhihuUser? = null,
        val error: ApiException? = null,
    )

    var uiState by mutableStateOf(PeopleUiState(isLoading = true))
        private set

    var headerScrollOffset by mutableFloatStateOf(0f)

    private var currentUrlToken: String? = null

    fun loadPeople(urlToken: String) {
        if (currentUrlToken == urlToken && !uiState.isLoading && uiState.user != null && uiState.error == null) return
        currentUrlToken = urlToken

        uiState = PeopleUiState(isLoading = true)

        viewModelScope.launch {
            repository.getUserDetail(urlToken)
                .onSuccess { user ->
                    uiState = uiState.copy(user = user, isLoading = false)
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    uiState = uiState.copy(error = e as? ApiException, isLoading = false)
                }
        }
    }
}

