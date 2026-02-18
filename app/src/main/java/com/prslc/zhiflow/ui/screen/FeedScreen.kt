package com.prslc.zhiflow.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prslc.zhiflow.data.exception.uiMessage
import com.prslc.zhiflow.ui.component.ErrorView
import com.prslc.zhiflow.ui.component.LoadMoreErrorItem
import com.prslc.zhiflow.ui.component.ZhihuFeedItem
import com.prslc.zhiflow.ui.viewmodel.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(),
    onItemClick: (String) -> Unit
) {

    val listState = viewModel.listState
    val apiError = viewModel.error

    LaunchedEffect(Unit) {
        viewModel.loadIfEmpty()
    }

    // load more
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex =
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 2
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && !viewModel.isNextLoading && !viewModel.isRefreshing) {
            viewModel.loadMore()
        }
    }

    if (apiError != null && viewModel.feedItems.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ErrorView(
                message = apiError.uiMessage,
                onRetry = { viewModel.refresh() }
            )
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = viewModel.isRefreshing && viewModel.feedItems.isNotEmpty(),
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        if (viewModel.isRefreshing && viewModel.feedItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = viewModel.feedItems,
                    key = { it.target?.id ?: it.hashCode() }
                ) { item ->
                    ZhihuFeedItem(item = item) { id ->
                        onItemClick(id)
                    }
                }

                if (viewModel.isNextLoading) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                } else if (apiError != null && viewModel.feedItems.isNotEmpty()) {
                    item {
                        LoadMoreErrorItem(
                            message = apiError.uiMessage,
                            onRetry = { viewModel.loadMore() }
                        )
                    }
                }
            }
        }
    }
}