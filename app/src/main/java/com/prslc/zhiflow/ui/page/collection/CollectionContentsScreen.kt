package com.prslc.zhiflow.ui.page.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.shouldLoadMore
import com.prslc.zhiflow.data.dto.CollectionItemDto
import com.prslc.zhiflow.ui.component.common.AuthorRow
import com.prslc.zhiflow.ui.component.common.ContentMeta
import com.prslc.zhiflow.ui.component.common.ContentTypeLabel
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.contentTypeConfig
import com.prslc.zhiflow.ui.component.common.pagingFooter
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionContentsScreen(
    uid: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionContentsViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current

    LaunchedEffect(uid) {
        viewModel.loadIfEmpty(uid)
    }

    val shouldLoadMore by remember { viewModel.listState.shouldLoadMore() }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    val items = viewModel.uiState.items
    val isRefreshing = viewModel.uiState.isRefreshing
    val globalError = viewModel.uiState.globalError
    val loadMoreError = viewModel.uiState.loadMoreError
    val isEmpty = items.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.collection_contents_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.general_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        modifier = modifier,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isEmpty && isRefreshing) {
                LoadingView(modifier = Modifier.fillMaxSize())
            } else if (isEmpty && globalError != null) {
                ErrorView(
                    message = globalError.uiMessage,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = viewModel.listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        itemsIndexed(
                            items = items,
                            key = { index, _ -> index },
                        ) { index, item ->
                            CollectionContentCard(
                                item = item,
                                onClick = { navigator.navigateToContent(item.id, item.type) },
                            )

                            if (index < items.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
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
            }
        }
    }
}

@Composable
private fun CollectionContentCard(
    item: CollectionItemDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            // type label + title
            Row(verticalAlignment = Alignment.CenterVertically) {
                val config = contentTypeConfig(item.type)
                ContentTypeLabel(
                    text = stringResource(config.labelResId),
                    containerColor = config.containerColor,
                    contentColor = config.contentColor,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AuthorRow(
                avatarUrl = item.authorAvatar,
                authorName = item.authorName,
                avatarSize = 20.dp,
                nameStyle = MaterialTheme.typography.labelMedium,
                nameColor = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.excerpt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ContentMeta(
                voteCount = item.voteCount,
                commentCount = item.commentCount,
            )

            if (item.collectionNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val folderText = if (item.collectionNames.size > 1) {
                    stringResource(
                        R.string.collection_contents_folder_many,
                        item.collectionNames.first(),
                        item.collectionNames.size,
                    )
                } else {
                    stringResource(
                        R.string.collection_contents_folder_one,
                        item.collectionNames.first(),
                    )
                }
                Text(
                    text = folderText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // cover image
        if (item.thumbnail != null) {
            Spacer(modifier = Modifier.width(12.dp))
            AsyncImage(
                model = item.thumbnail,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 100.dp, height = 68.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
