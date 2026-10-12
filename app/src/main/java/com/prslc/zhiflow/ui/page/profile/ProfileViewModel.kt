package com.prslc.zhiflow.ui.page.profile

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.ignoreOutcome
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.core.network.CredentialStore
import com.prslc.zhiflow.data.model.user.ZhihuUser
import com.prslc.zhiflow.data.repository.AuthRepository
import com.prslc.zhiflow.data.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: UserRepository,
    private val credentialStore: CredentialStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    @Immutable
    data class UserUiState(
        val isLoading: Boolean = false,
        val user: ZhihuUser? = null,
        val globalError: ApiException? = null,
        /** True with only an anonymous session behind it, where there is no profile to fetch. */
        val isGuest: Boolean = false,
    )

    var uiState by mutableStateOf(UserUiState())
        private set

    /** The session [UserUiState.user] was loaded under, so a different one can drop it. */
    private var loadedToken: String? = null

    init {
        loadProfile()
        viewModelScope.launch {
            credentialStore.session.collect { session ->
                if (session.accessToken != loadedToken) loadProfile()
            }
        }
    }

    /**
     * Loads the signed-in user's profile, or settles on the anonymous state that has none.
     *
     * Runs at construction and again whenever the session changes. Signing in, signing out and
     * dropping to a guest all write the store, and not one of them is a screen this one could
     * have asked about it.
     */
    fun loadProfile() {
        val session = credentialStore.read()
        loadedToken = session.accessToken

        // `people/self` is not answered for an anonymous session; `/guest/self` is. An empty
        // store is that same state one request early — the funnel bootstraps a guest underneath
        // — so both settle here.
        if (session.isGuest || !session.hasToken) {
            uiState = UserUiState(isGuest = true)
            loadGuest(session.accessToken)
            return
        }

        uiState = uiState.copy(isLoading = true, isGuest = false)
        viewModelScope.launch {
            val outcome = repository.getMyDetail()
            // A session that changed while this was in flight has already replaced the state;
            // letting the answer land would put back the very profile it dropped.
            if (session.accessToken != loadedToken) return@launch

            outcome
                .onSuccess { user ->
                    uiState = uiState.copy(user = user, isLoading = false)
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(globalError = error, isLoading = false)
                }
        }
    }

    /**
     * Fetches the anonymous session's own profile, which is where its default avatar comes from.
     *
     * Nothing is reported when this fails. What is behind it is the same page without a picture,
     * and a picture is not worth turning the page into an error.
     */
    private fun loadGuest(token: String) {
        viewModelScope.launch {
            val outcome = repository.getGuestDetail()
            if (token != loadedToken) return@launch

            outcome.onSuccess { guest ->
                uiState = UserUiState(isGuest = true, user = guest)
            }
        }
    }

    /**
     * Ends the session, here and on the server.
     *
     * Nothing comes back to the caller: clearing the store is what this tab draws from, so it
     * settles into the anonymous state on its own. A server that could not be told is not worth
     * reporting either — the session is gone from this device either way, and the guest
     * bootstrap underneath replaces the cookie that would otherwise have outlived it.
     */
    fun signOut() {
        viewModelScope.launch { authRepository.logout().ignoreOutcome() }
    }
}
