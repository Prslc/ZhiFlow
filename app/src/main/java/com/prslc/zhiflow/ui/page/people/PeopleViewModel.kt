package com.prslc.zhiflow.ui.page.people

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.model.ZhihuUser
import com.prslc.zhiflow.data.repository.UserRepository
import kotlinx.coroutines.launch

class PeopleViewModel(private val repository: UserRepository) : ViewModel() {

    /**
     * UI state for the people
     */
    sealed interface PeopleUiState {
        data object Loading : PeopleUiState
        data class Success(val user: ZhihuUser) : PeopleUiState
        data class Error(val throwable: Throwable) : PeopleUiState
    }

    var uiState by mutableStateOf<PeopleUiState>(PeopleUiState.Loading)
        private set

    fun loadPeople(urlToken: String) {

        uiState = PeopleUiState.Loading

        viewModelScope.launch {
            repository.getUserDetail(urlToken)
                .onSuccess { user ->
                    uiState = PeopleUiState.Success(user)
                }
                .onFailure { e ->
                    uiState = PeopleUiState.Error(e)
                }
        }
    }
}