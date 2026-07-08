package com.prslc.zhiflow.ui.page.people.moment

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.dto.MomentDto
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.data.mapper.toDto
import com.prslc.zhiflow.data.model.moment.ComponentCard
import com.prslc.zhiflow.data.model.moment.MomentsFeedItem
import com.prslc.zhiflow.data.model.moment.MomentsPage
import com.prslc.zhiflow.data.repository.MomentRepository
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

open class MomentViewModel<T>(
    val tabKeyPrefix: String,
    private val repository: MomentRepository,
    private val fetchSource: suspend (MomentRepository, String, String?) -> Result<MomentsPage<T>>,
    private val toItemState: (T) -> MomentDto,
) : ViewModel() {

    @Immutable
    data class MomentUiState(
        val isLoading: Boolean = false,
        val isNextLoading: Boolean = false,
        val moments: List<MomentDto> = emptyList(),
        val error: ApiException? = null,
    )

    var uiState by mutableStateOf(MomentUiState(isLoading = true))
        private set

    val listState = LazyListState()

    private var nextUrl: String? = null
    private var isEnd: Boolean = false
    private var currentUrlToken: String? = null
    private var lastLoadedToken: String? = null

    fun loadIfNeeded(urlToken: String) {
        if (lastLoadedToken == urlToken) return
        lastLoadedToken = urlToken
        loadMoment(urlToken)
    }

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

                    val cleanMoments = response.data.mapNotNull { item ->
                        runCatching { toItemState(item) }.getOrNull()
                    }
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

                    val moreCleanMoments = response.data.mapNotNull { item ->
                        runCatching { toItemState(item) }.getOrNull()
                    }
                    uiState = uiState.copy(
                        moments = uiState.moments + moreCleanMoments,
                        isNextLoading = false,
                        error = null,
                    )
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    uiState = uiState.copy(
                        error = e as? ApiException ?: ApiException.UnknownException(),
                        isNextLoading = false,
                    )
                }
        }
    }
}

class PostsViewModel(repository: MomentRepository) :
    MomentViewModel<ComponentCard>(
        tabKeyPrefix = "post",
        repository = repository,
        fetchSource = { repo, token, nextUrl -> repo.getUserPost(token, nextUrl) },
        toItemState = { it.toDto() },
    )

class ActivitiesViewModel(repository: MomentRepository) :
    MomentViewModel<MomentsFeedItem>(
        tabKeyPrefix = "activity",
        repository = repository,
        fetchSource = { repo, token, nextUrl -> repo.getUserActivities(token, nextUrl) },
        toItemState = { it.toDto() },
    )

class UpvotesViewModel(repository: MomentRepository) :
    MomentViewModel<MomentsFeedItem>(
        tabKeyPrefix = "upvote",
        repository = repository,
        fetchSource = { repo, token, nextUrl -> repo.getUserVote(token, nextUrl) },
        toItemState = { it.toDto() },
    )
