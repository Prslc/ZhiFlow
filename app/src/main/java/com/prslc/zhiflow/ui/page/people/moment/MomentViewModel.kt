package com.prslc.zhiflow.ui.page.people.moment

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.data.mapper.toItemState
import com.prslc.zhiflow.data.model.MomentsResponse
import com.prslc.zhiflow.data.repository.MomentRepository
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

open class MomentViewModel(
    val tabKeyPrefix: String,
    private val repository: MomentRepository,
    private val fetchSource: suspend (repository: MomentRepository, urlToken: String, nextUrl: String?) -> Result<MomentsResponse>
) : ViewModel() {

    @Immutable
    data class MomentUiState(
        val isLoading: Boolean = false,
        val isNextLoading: Boolean = false,
        val moments: List<MomentItemState> = emptyList(),
        val error: ApiException? = null
    )

    var uiState by mutableStateOf(MomentUiState())
        private set

    val listState = LazyListState()

    private var nextUrl: String? = null
    private var isEnd: Boolean = false
    private var currentUrlToken: String? = null

    fun loadMoment(urlToken: String) {
        currentUrlToken = urlToken
        nextUrl = null
        isEnd = false
        uiState = MomentUiState(isLoading = true, error = null)

        viewModelScope.launch {
            fetchSource(repository, urlToken, null)
                .onSuccess { response ->
                    nextUrl = response.paging.next
                    isEnd = response.paging.isEnd

                    val cleanMoments = response.data.map { it.toItemState() }
                    uiState = uiState.copy(moments = cleanMoments, isLoading = false)
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    uiState = uiState.copy(error = e as? ApiException, isLoading = false)
                }
        }
    }

    fun loadMore() {
        val token = currentUrlToken ?: return
        if (isEnd || uiState.isNextLoading) return

        uiState = uiState.copy(isNextLoading = true)

        viewModelScope.launch {
            fetchSource(repository, token, nextUrl)
                .onSuccess { response ->
                    nextUrl = response.paging.next
                    isEnd = response.paging.isEnd

                    val moreCleanMoments = response.data.map { it.toItemState() }
                    uiState = uiState.copy(
                        moments = uiState.moments + moreCleanMoments,
                        isNextLoading = false
                    )
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    uiState = uiState.copy(
                        error = e as? ApiException,
                        isNextLoading = false
                    )
                }
        }
    }
}

class PostsViewModel(repository: MomentRepository) :
    MomentViewModel(
        tabKeyPrefix = "post",
        repository = repository,
        fetchSource = { repo, token, nextUrl -> repo.getUserPost(token, nextUrl) }
    )

class ActivitiesViewModel(repository: MomentRepository) :
    MomentViewModel(
        tabKeyPrefix = "activity",
        repository = repository,
        fetchSource = { repo, token, nextUrl -> repo.getUserActivities(token, nextUrl) }
    )

class UpvotesViewModel(repository: MomentRepository) :
    MomentViewModel(
        tabKeyPrefix = "upvote",
        repository = repository,
        fetchSource = { repo, token, nextUrl -> repo.getUserVote(token, nextUrl) }
    )