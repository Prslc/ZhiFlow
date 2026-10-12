package com.prslc.zhiflow.ui.page.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.BarCollapseDistance
import com.prslc.zhiflow.core.utils.compose.rememberHideOnScroll
import com.prslc.zhiflow.core.utils.compose.shouldLoadMore
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.pagingFooter
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface
import org.koin.androidx.compose.koinViewModel

/**
 * The search field's height, and so the distance over which scrolling it away takes.
 *
 * The space below the field belongs to the list rather than to the field, so that it is still
 * there once the field has gone. Inside the field it would be clipped along with it, and a
 * half-collapsed bar would show a flat edge, then its inset, then the card's own padding — two
 * gaps where there is one.
 */
private val SearchBarHeight = 50.dp

/** The row's own height, inside the space above it. */
private val SearchEntryHeight = 44.dp

/** Space the field keeps above itself, and the list keeps below it. */
private val SearchBarInset = 4.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeedScreen(
    onItemLongClick: (String, String) -> Unit,
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

    val navigator = LocalNavigator.current

    // Held across recompositions rather than written inline: the list is keyed on these two, and a
    // lambda that changes identity takes every row down with it. The long press comes from the
    // caller rather than being bound to this page's ViewModel here: a composable that takes a
    // ViewModel may not hand anything derived from it down to another one — which is the screen
    // that draws the sheet the long press opens.
    val stableOnItemClick: (String, String) -> Unit =
        remember(navigator) { navigator::navigateToContent }
    val stableOnItemLongClick: (String, String) -> Unit =
        remember(onItemLongClick) { onItemLongClick }

    val items = viewModel.uiState.items
    val isRefreshing = viewModel.uiState.isRefreshing
    val globalError = viewModel.uiState.globalError
    val loadMoreError = viewModel.uiState.loadMoreError
    val isEmpty = items.isEmpty()
    val endText = if (viewModel.uiState.isEnd) stringResource(R.string.end_of_list) else null

    val searchBar = rememberHideOnScroll(BarCollapseDistance)
    val barHeightPx = with(LocalDensity.current) { SearchBarHeight.toPx() }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SearchBarHeight * (1f - searchBar.fraction))
                .clipToBounds(),
        ) {
            SearchEntry(
                onClick = { navigator.navigateToSearch() },
                modifier = Modifier.graphicsLayer {
                    translationY = -barHeightPx * searchBar.fraction
                },
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(searchBar.connection),
        ) {
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
                },
            ) {
                LazyColumn(
                    state = viewModel.listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = SearchBarInset, bottom = 16.dp),
                ) {
                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.id },
                    ) { index, item ->
                        FeedItem(
                            display = item,
                            onClick = stableOnItemClick,
                            onLongClick = stableOnItemLongClick,
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
                            endText = endText,
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
}

@Composable
private fun SearchEntry(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = SearchBarInset)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .height(SearchEntryHeight)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.search_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The way into the search page, which is the feed's own top bar entry. */
@AppPreviews
@Composable
private fun SearchEntryPreview() {
    PreviewSurface {
        SearchEntry(onClick = {})
    }
}
