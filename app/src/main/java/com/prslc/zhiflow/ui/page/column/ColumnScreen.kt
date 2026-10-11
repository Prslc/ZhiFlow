package com.prslc.zhiflow.ui.page.column

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.shouldLoadMore
import com.prslc.zhiflow.core.utils.formatToDate
import com.prslc.zhiflow.data.dto.ColumnDto
import com.prslc.zhiflow.data.dto.ColumnItemDto
import com.prslc.zhiflow.ui.component.LocalDataSaver
import com.prslc.zhiflow.ui.component.common.EmptyView
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.FollowButton
import com.prslc.zhiflow.ui.component.common.ImageOmitted
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.StatItem
import com.prslc.zhiflow.ui.component.common.pagingFooter
import com.prslc.zhiflow.ui.component.common.rememberActionErrorHost
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import org.koin.androidx.compose.koinViewModel

private val AvatarSize = 56.dp
private val Gutter = 20.dp

/**
 * One column: what it is, and the articles it holds.
 *
 * The header carries the column's own counts and the follow toggle; under it the articles read as
 * the feed's rows do, and each opens the article itself.
 *
 * @param urlToken The column to open.
 * @param onBack Called by the back arrow.
 * @param modifier Applied to the scaffold.
 * @param viewModel The page's own; a caller passes one only to test.
 */
@Composable
fun ColumnScreen(
    urlToken: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ColumnViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    LaunchedEffect(urlToken) { viewModel.loadColumn(urlToken) }

    val shouldLoadMore by remember { viewModel.listState.shouldLoadMore() }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    val uiState = viewModel.uiState
    val column = uiState.column
    val snackbarHostState = rememberActionErrorHost(
        viewModel.actionError,
        viewModel::consumeActionError,
    )

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = column?.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                column == null && uiState.error != null -> {
                    ErrorView(
                        message = uiState.error.uiMessage,
                        onRetry = viewModel::retry,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                column == null -> {
                    LoadingView(modifier = Modifier.fillMaxSize())
                }

                else -> {
                    val endText = if (uiState.isEnd && uiState.items.isNotEmpty()) {
                        stringResource(R.string.end_of_list)
                    } else {
                        null
                    }

                    LazyColumn(
                        state = viewModel.listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        item(key = "header", contentType = "ColumnHeader") {
                            ColumnHeader(
                                column = column,
                                onFollowClick = viewModel::toggleFollow,
                                onAuthorClick = {
                                    navigator.navigateToPeople(
                                        column.author.urlToken.ifBlank { column.author.id },
                                    )
                                },
                            )
                        }

                        items(uiState.items, key = { it.id }) { item ->
                            ColumnArticleRow(
                                item = item,
                                onClick = { navigator.navigateToContent(item.id, "article") },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = Gutter),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                        }

                        if (uiState.items.isEmpty() &&
                            !uiState.isNextLoading &&
                            uiState.loadMoreError == null
                        ) {
                            item(key = "empty", contentType = "ColumnEmpty") {
                                EmptyView(
                                    message = stringResource(R.string.column_empty),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                )
                            }
                        }

                        pagingFooter(
                            isLoading = uiState.isNextLoading,
                            error = uiState.loadMoreError,
                            onRetry = viewModel::loadMore,
                            endText = endText,
                        )
                    }
                }
            }
        }
    }
}

/**
 * What a column is, above its articles: its picture, its name, the line it introduces itself with,
 * its author and its counts, with the follow toggle.
 */
@Composable
private fun ColumnHeader(
    column: ColumnDto,
    onFollowClick: () -> Unit,
    onAuthorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Gutter, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = column.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = column.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.width(12.dp))

            FollowButton(
                isFollowing = column.isFollowing,
                onClick = onFollowClick,
                compact = true,
                followLabel = stringResource(R.string.column_action_subscribe),
                followingLabel = stringResource(R.string.column_action_subscribed),
            )
        }

        if (column.intro.isNotBlank()) {
            Text(
                text = column.intro,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatItem(stringResource(R.string.column_stat_followers), column.followers)
            StatItem(stringResource(R.string.column_stat_articles), column.articlesCount)
            StatItem(stringResource(R.string.column_stat_voteup), column.voteupCount)
        }

        if (column.author.name.isNotBlank()) {
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clickable(onClick = onAuthorClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = column.author.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentScale = ContentScale.Crop,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = column.author.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

/** One article of the column, drawn as the feed's rows are: title, excerpt, counts, and a cover. */
@Composable
private fun ColumnArticleRow(
    item: ColumnItemDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Gutter, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.excerpt.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = buildString {
                    if (item.created > 0) {
                        append(formatToDate(item.created))
                        append("  ")
                    }
                    append(
                        pluralStringResource(
                            R.plurals.feed_meta,
                            item.commentCount,
                            item.voteupCount,
                            item.commentCount,
                        ),
                    )
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
            )
        }

        if (item.imageUrl != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 68.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            ) {
                // An illustration in a body and a list's thumbnails read the reader's choice; this
                // is a list's thumbnail.
                if (!LocalDataSaver.current) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    ImageOmitted()
                }
            }
        }
    }
}
