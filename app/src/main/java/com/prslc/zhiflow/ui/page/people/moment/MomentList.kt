package com.prslc.zhiflow.ui.page.people.moment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.data.model.MediaImage
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadMoreErrorItem

enum class MomentContentType {
    ANSWER,
    ARTICLE,
    THOUGHT,
    UNKNOWN
}

@Immutable
data class MomentItemState(
    val id: String,
    val type: MomentContentType,
    val title: String,
    val plainContent: String,
    val summary: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val routerUrl: String?,
    val voteCount: Int,
    val commentCount: Int,
    val collectCount: Int,
    val mediaImages: List<MediaImage>,
    val videoThumbnail: String?,
    val publishedAt: Long,
    val actionText: String,
    val actionTime: Long,
    val isTopping: Boolean
)

@Composable
fun AutoLoadMoreEffect(viewModel: MomentViewModel, selectedTab: Int) {
    if (selectedTab != 1) return

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = viewModel.listState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }
}

fun LazyListScope.momentsContent(
    urlToken: String,
    state: MomentViewModel.MomentUiState,
    viewModel: MomentViewModel
) {
    when {
        state.isLoading && state.moments.isEmpty() -> {
            item(key = "initial_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        state.error != null && state.moments.isEmpty() -> {
            item(key = "initial_error") {
                ErrorView(
                    message = state.error.uiMessage,
                    onRetry = { viewModel.loadMoment(urlToken) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        else -> {
            itemsIndexed(items = state.moments, key = { _, item -> item.id }) { _, item ->
                MomentCard(state = item, modifier = Modifier.fillMaxWidth())
            }
            pagingFooter(
                isLoading = state.isNextLoading,
                error = state.error.takeIf { state.moments.isNotEmpty() },
                onRetry = { viewModel.loadMore() }
            )
        }
    }
}

private fun LazyListScope.pagingFooter(
    isLoading: Boolean,
    error: Throwable?,
    onRetry: () -> Unit
) {
    if (isLoading) {
        item(key = "footer_loading") {
            Box(Modifier
                .fillMaxWidth()
                .padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    } else if (error != null) {
        item(key = "footer_error") {
            val message = if (error is ApiException) error.uiMessage else error.message
                ?: stringResource(R.string.error_unknown)
            LoadMoreErrorItem(message = message, onRetry = onRetry)
        }
    }
}
