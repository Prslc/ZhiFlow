package com.prslc.zhiflow.ui.page.profile

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.core.network.CredentialStore
import com.prslc.zhiflow.data.model.user.ZhihuUser
import com.prslc.zhiflow.data.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: UserRepository,
    private val credentialStore: CredentialStore,
) : ViewModel() {

    @Immutable
    data class UserUiState(
        val isLoading: Boolean = false,
        val user: ZhihuUser? = null,
        val error: ApiException? = null,

        /** True with only an anonymous session behind it, where there is no profile to fetch. */
        val isGuest: Boolean = false,
    )

    var uiState by mutableStateOf(UserUiState())
        private set

    fun loadProfile() {
        // `people/self` is not answered for an anonymous session, so asking would only turn a
        // known state into an error.
        if (credentialStore.read().isGuest) {
            uiState = UserUiState(isGuest = true)
            return
        }

        uiState = uiState.copy(isLoading = true, isGuest = false)
        viewModelScope.launch {
            repository.getMyDetail()
                .onSuccess { user ->
                    uiState = uiState.copy(user = user, isLoading = false)
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(error = error, isLoading = false)
                }
        }
    }
}
