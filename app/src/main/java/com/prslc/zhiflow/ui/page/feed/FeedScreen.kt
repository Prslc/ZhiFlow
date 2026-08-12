package com.prslc.zhiflow.ui.page.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.shouldLoadMore
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.pagingFooter
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshing && !isEmpty,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing && !isEmpty,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            LazyColumn(
                state = viewModel.listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.id },
                ) { index, item ->
                    FeedItem(
                        display = item,
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
