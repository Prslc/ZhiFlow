package com.prslc.zhiflow.ui.component.comment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.data.model.ZhihuComment

@Composable
fun CommentList(
    comments: List<ZhihuComment>,
    isLoading: Boolean,             // track loading state
    hasMore: Boolean,               // check if pagination ended
    onLoadMore: () -> Unit,         // trigger next page
    modifier: Modifier = Modifier,
    onAuthorClick: (String) -> Unit = {},
    onLikeClick: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {}
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > (totalItemsNumber - 3) && totalItemsNumber > 0
        }
    }

    LaunchedEffect(shouldLoadMore, isLoading) {
        if (shouldLoadMore && !isLoading && hasMore) {
            onLoadMore()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState
    ) {
        items(
            items = comments,
            key = { it.id }
        ) { comment ->
            CommentItem(
                comment = comment,
                onAuthorClick = onAuthorClick,
                onLikeClick = onLikeClick,
                onImageClick = onImageClick
            )

            HorizontalDivider(
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        // Loading Footer
        if (isLoading && hasMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}