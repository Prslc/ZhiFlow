package com.prslc.zhiflow.ui.page.history

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.shouldLoadMore
import com.prslc.zhiflow.data.dto.ReadHistoryDto
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.pagingFooter
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReadHistoryViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    LaunchedEffect(Unit) { viewModel.loadIfEmpty() }

    val shouldLoadMore by remember { viewModel.listState.shouldLoadMore() }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    val items = viewModel.uiState.items
    val isRefreshing = viewModel.uiState.isRefreshing
    val globalError = viewModel.uiState.globalError
    val loadMoreError = viewModel.uiState.loadMoreError
    val isEmpty = items.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.general_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Search Action */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
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
                LazyColumn(
                    state = viewModel.listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    itemsIndexed(
                        items = items,
                        key = { index, _ -> index },
                    ) { idx, item ->
                        when (item) {
                            is HistoryListItem.Header -> {
                                HistoryHeader(
                                    header = item,
                                    onClick = { navigator.navigateToContent(item.questionToken, "question") },
                                )
                            }

                            is HistoryListItem.Entry -> {
                                HistoryItem(
                                    dto = item.dto,
                                    isInGroup = item.isInGroup,
                                    onClick = {
                                        navigator.navigateToContent(
                                            item.dto.contentToken.orEmpty(),
                                            item.dto.contentType.orEmpty(),
                                        )
                                    },
                                )
                            }
                        }

                        if (idx < items.lastIndex) {
                            val next = items[idx + 1]

                            when {
                                item is HistoryListItem.Header && next is HistoryListItem.Entry && next.isInGroup -> {}

                                item is HistoryListItem.Entry && item.isInGroup && next is HistoryListItem.Entry && next.isInGroup -> {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 36.dp, end = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                }

                                next is HistoryListItem.Header -> {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceContainerLow.copy(
                                                    alpha = 0.5f
                                                )
                                            )
                                    )
                                }

                                item is HistoryListItem.Entry && next is HistoryListItem.Entry -> {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                    if (!isEmpty) {
                        pagingFooter(
                            isLoading = viewModel.uiState.isNextLoading,
                            error = loadMoreError,
                            isEnd = viewModel.uiState.isEnd,
                            onRetry = { viewModel.loadMore() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeader(
    header: HistoryListItem.Header,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (header.icon != null) {
            AsyncImage(
                model = header.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = header.questionTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HistoryItem(
    dto: ReadHistoryDto,
    isInGroup: Boolean,
    onClick: () -> Unit,
) {
    val hasContent = dto.contentType in listOf("answer", "article", "pin")
    val startPadding = if (isInGroup) 36.dp else 16.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .padding(start = startPadding, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // title
            if (!isInGroup && dto.questionTitle.isNotEmpty()) {
                Row(verticalAlignment = Alignment.Top) {
                    if (dto.contentTypeIcon != null) {
                        AsyncImage(
                            model = dto.contentTypeIcon,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = dto.questionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // content
            if (hasContent) {
                val summaryText = buildAnnotatedString {
                    val author = dto.authorName?.ifEmpty { null }
                    if (author != null) {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append("$author：")
                        }
                    }
                    if (!dto.summary.isNullOrBlank()) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(dto.summary)
                        }
                    }
                }
                if (summaryText.isNotEmpty()) {
                    Text(
                        text = summaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(8.dp))

            // metadata
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val metaText = when (dto.contentType) {
                    "question" -> stringResource(
                        R.string.history_question_meta,
                        dto.answerCount,
                        dto.followerCount
                    )

                    "profile" -> stringResource(
                        R.string.history_user_meta,
                        dto.voteCount,
                        dto.followerCount
                    )

                    else -> stringResource(R.string.history_meta, dto.voteCount, dto.commentCount)
                }

                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )

                if (hasContent && dto.readProgress > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.history_read_progress, dto.readProgress),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // coverImage
        if (hasContent && dto.coverImage != null) {
            Spacer(modifier = Modifier.width(12.dp))
            AsyncImage(
                model = dto.coverImage,
                contentDescription = null,
                modifier = Modifier
                    .size(width = 100.dp, height = 68.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}