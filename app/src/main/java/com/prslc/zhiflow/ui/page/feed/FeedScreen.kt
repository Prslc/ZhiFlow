package com.prslc.zhiflow.ui.page.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.shouldLoadMore
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadMoreErrorItem
import com.prslc.zhiflow.ui.component.common.LoadingView
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onItemClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.loadIfEmpty()
    }

    val shouldLoadMore by remember { viewModel.listState.shouldLoadMore() }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    val stableOnItemClick = remember(onItemClick) { onItemClick }

    val items = viewModel.uiState.items
    val isRefreshing = viewModel.uiState.isRefreshing
    val globalError = viewModel.uiState.globalError
    val loadMoreError = viewModel.uiState.loadMoreError
    val isEmpty = items.isEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing && !isEmpty,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = viewModel.listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(
                    items = items,
                    key = { index, item -> item.target?.id ?: "feed_fallback_index_$index" },
                    contentType = { _, _ -> "FeedItem" }
                ) { index, item ->
                    FeedItem(
                        item = item,
                        onClick = stableOnItemClick,
                    )

                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
                if (!isEmpty) {
                    pagingFooter(
                        isLoading = viewModel.uiState.isNextLoading,
                        error = loadMoreError,
                        onRetry = { viewModel.loadMore() },
                    )
                }
            }
        }

        if (isEmpty && isRefreshing) {
            LoadingView(modifier = Modifier.fillMaxSize())
        }

        if (isEmpty && globalError != null && !isRefreshing) {
            ErrorView(
                message = globalError.uiMessage,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
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
        item(key = "footer_loading", contentType = "PagingFooterLoading") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp), contentAlignment = Alignment.Center
            ) {
                LoadingView(modifier = Modifier.size(24.dp))
            }
        }
    } else if (error != null) {
        item(key = "footer_error", contentType = "PagingFooterError") {
            val message = if (error is ApiException) {
                error.uiMessage
            } else {
                error.message ?: stringResource(R.string.error_unknown)
            }
            LoadMoreErrorItem(message = message, onRetry = onRetry)
        }
    }
}
