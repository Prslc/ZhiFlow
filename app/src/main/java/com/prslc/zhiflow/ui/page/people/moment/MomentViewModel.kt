package com.prslc.zhiflow.ui.page.people.moment

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.data.mapper.toItemState
import com.prslc.zhiflow.data.repository.MomentRepository
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class MomentViewModel(private val repository: MomentRepository) : ViewModel() {

    @Stable
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
            repository.getMoment(urlToken)
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
            repository.getMoment(token, nextUrl)
                .onSuccess { response ->
                    nextUrl = response.paging.next
                    isEnd = response.paging.isEnd

                    // 💡 同样在这里完成增量数据的转换
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