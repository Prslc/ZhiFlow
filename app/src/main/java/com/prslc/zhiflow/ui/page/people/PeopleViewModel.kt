package com.prslc.zhiflow.ui.page.people

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.ignoreOutcome
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.data.model.user.ReadHistoryRequest
import com.prslc.zhiflow.data.model.user.ZhihuUser
import com.prslc.zhiflow.data.repository.ActionRepository
import com.prslc.zhiflow.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** What the history calls a profile visit. It is not a content type: a profile is not content. */
private const val PROFILE_CONTENT_TYPE = "profile"

class PeopleViewModel(
    private val repository: UserRepository,
    private val actionRepository: ActionRepository,
) : ViewModel() {

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
                    recordVisit(user.id)
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(error = error, isLoading = false)
                }
        }
    }

    /**
     * Puts the visit in the reader's history.
     *
     * A profile has no reading progress, and the entry is written on arrival rather than on the way
     * out — which is why this does not go through the progress the content screens keep. The history
     * addresses the reader by the account's own id, not by the url token the page was opened with.
     */
    private fun recordVisit(userId: String) {
        if (userId.isEmpty()) return

        viewModelScope.launch {
            actionRepository.syncHistory(
                ReadHistoryRequest(
                    contentToken = userId,
                    contentType = PROFILE_CONTENT_TYPE,
                    readProgress = 0,
                )
            ).ignoreOutcome()
        }
    }

    private var followJob: Job? = null

    /** Set when a follow toggle fails, so the screen can explain the rollback. */
    var actionError by mutableStateOf<ApiException?>(null)
        private set

    fun consumeActionError() {
        actionError = null
    }

    /**
     * Optimistically toggles the follow state of the loaded user,
     * rolling back on API failure.
     */
    fun toggleFollow() {
        val user = uiState.user ?: return
        if (user.id.isEmpty()) return
        if (followJob?.isActive == true) return

        val target = !(user.isFollowing == true)
        uiState = uiState.copy(user = user.copy(isFollowing = target))
        followJob = viewModelScope.launch {
            val result = if (target) repository.followUser(user.id) else repository.unfollowUser(user.id)
            result.onApiFailure { error ->
                uiState = uiState.copy(user = uiState.user?.copy(isFollowing = !target))
                actionError = error
            }
        }
    }
}

