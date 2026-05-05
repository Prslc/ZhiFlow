package com.prslc.zhiflow.ui.page.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
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
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadMoreErrorItem
import com.prslc.zhiflow.ui.component.common.LoadingView
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = koinViewModel(),
    onItemClick: (String, String) -> Unit
) {
    // init
    AutoLoadMoreEffect(viewModel)

    val onRefresh = remember { { viewModel.refresh() } }
    val onLoadMoreRetry = remember { { viewModel.loadMore() } }

    Box(Modifier.fillMaxSize()) {
        val items = viewModel.uiState.items
        val isRefreshing = viewModel.uiState.isRefreshing
        val isEmpty = items.isEmpty()

        PullToRefreshBox(
            isRefreshing = isRefreshing && !isEmpty,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = viewModel.listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = items,
                    key = { it.target?.id ?: it.hashCode() }
                ) { item ->
                    FeedItem(item = item) { id, type ->
                        onItemClick(id, type)
                    }
                }

                if (!isEmpty) {
                    pagingFooter(
                        isLoading = viewModel.uiState.isNextLoading,
                        error = viewModel.uiState.error,
                        onRetry = onLoadMoreRetry
                    )
                }
            }
        }

        if (isEmpty && isRefreshing) {
            LoadingView(modifier = Modifier.fillMaxSize())
        }

        val error = viewModel.uiState.error
        if (isEmpty && error != null && !isRefreshing) {
            ErrorView(
                message = error.uiMessage,
                onRetry = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
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
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    } else if (error != null) {
        item(key = "footer_error") {
            val message = if (error is ApiException) {
                error.uiMessage
            } else {
                error.message ?: stringResource(R.string.error_unknown)
            }
            LoadMoreErrorItem(
                message = message,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun AutoLoadMoreEffect(viewModel: FeedViewModel) {
    val listState = viewModel.listState

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        val currentState = viewModel.uiState

        if (shouldLoadMore && !currentState.isNextLoading && !currentState.isRefreshing) {
            viewModel.loadMore()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadIfEmpty()
    }
}