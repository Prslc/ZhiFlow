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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.prslc.zhiflow.core.utils.compose.sizedImageUrl
import com.prslc.zhiflow.core.utils.formatToDate
import com.prslc.zhiflow.data.dto.ColumnDto
import com.prslc.zhiflow.data.dto.ColumnItemDto
import com.prslc.zhiflow.ui.component.LocalDataSaver
import com.prslc.zhiflow.ui.component.common.ContentTypeLabel
import com.prslc.zhiflow.ui.component.common.EmptyView
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.FollowButton
import com.prslc.zhiflow.ui.component.common.FollowToggle
import com.prslc.zhiflow.ui.component.common.ImageOmitted
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.StatItem
import com.prslc.zhiflow.ui.component.common.contentTypeConfig
import com.prslc.zhiflow.ui.component.common.pagingFooter
import com.prslc.zhiflow.ui.component.common.rememberActionErrorHost
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import org.koin.androidx.compose.koinViewModel

private val AvatarSize = 56.dp
private val Gutter = 20.dp
private val CoverShape = RoundedCornerShape(6.dp)

/** How much of the primary color the author's follow button washes its own fill with. */
private const val TINT_ALPHA = 0.08f

/**
 * One column: what it is, and the content it holds.
 *
 * The header carries the column's own counts and the follow toggle; under it the entries read as
 * the feed's rows do, and each opens the article or the answer it names.
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
            when (column) {
                null if uiState.globalError != null -> {
                    ErrorView(
                        message = uiState.globalError.uiMessage,
                        onRetry = viewModel::retry,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                null -> {
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
                                onAuthorFollowClick = viewModel::toggleAuthorFollow,
                            )
                        }

                        items(uiState.items, key = { it.id }) { item ->
                            ColumnItemRow(
                                item = item,
                                onClick = { navigator.navigateToContent(item.id, item.type) },
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
 * What a column is, above its list: its picture, its name, the line it introduces itself with,
 * its author and its counts, with the follow toggle.
 */
@Composable
private fun ColumnHeader(
    column: ColumnDto,
    onFollowClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onAuthorFollowClick: () -> Unit,
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
            StatItem(stringResource(R.string.column_stat_views), column.viewsCount)
            StatItem(stringResource(R.string.column_stat_voteup), column.voteupCount)
            StatItem(stringResource(R.string.column_stat_followers), column.followers)
        }

        if (column.author.name.isNotBlank()) {
            Text(
                text = stringResource(R.string.column_author_heading),
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clickable(onClick = onAuthorClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = sizedImageUrl(column.author.avatarUrl, 32.dp),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentScale = ContentScale.Crop,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = column.author.name,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AuthorFollowButton(
                            isFollowing = column.author.isFollowing,
                            onClick = onAuthorFollowClick,
                        )
                    }
                    if (column.author.headline.isNotBlank()) {
                        Text(
                            text = column.author.headline,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            ColumnInfluence(column)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

/**
 * The author's follow button, as this page draws it: a wash of the primary color with that color
 * written on it, and a plus.
 *
 * The wash is what the page's own buttons wear — about an eighth of the way from the surface to the
 * primary. It reads by its words rather than by its fill, which is what keeps it legible under the
 * dynamic palette, where a container and its surface can sit within a few steps of each other.
 *
 * Its size is capped from the outside: M3's Button enforces 58x40dp minimums on its inner content
 * row, and such a default only fills in where the incoming minimum is zero — so a height and a
 * width of this page's own are what get under it.
 */
@Composable
private fun AuthorFollowButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FollowToggle(
        isFollowing = isFollowing,
        onClick = onClick,
        modifier = modifier,
        label = "AuthorFollowButton",
    ) { following, onToggle ->
        Button(
            onClick = onToggle,
            modifier = Modifier
                .height(22.dp)
                .widthIn(min = 44.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (following) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = TINT_ALPHA)
                },
                contentColor = if (following) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            ),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Icon(
                imageVector = if (following) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = stringResource(
                    if (following) R.string.people_action_following else R.string.people_action_follow,
                ),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/** One entry of the column, drawn as the feed's rows are: title, excerpt, counts, and a cover. */
@Composable
private fun ColumnItemRow(
    item: ColumnItemDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeConfig = contentTypeConfig(item.type)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Gutter, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ContentTypeLabel(
                    text = stringResource(typeConfig.labelResId),
                    containerColor = typeConfig.containerColor,
                    contentColor = typeConfig.contentColor,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = pluralStringResource(
                        R.plurals.feed_meta,
                        item.commentCount,
                        item.voteupCount,
                        item.commentCount,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                )

                if (item.created > 0) {
                    Text(
                        text = formatToDate(item.created),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                    )
                }
            }
        }

        if (item.imageUrl != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 68.dp)
                    .clip(CoverShape)
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

/**
 * The author's standing, under their name: the line's title and, beneath it, the counts we worded
 * ourselves — or the server's own words for them, where they could not be read.
 *
 * Nothing is drawn when it said neither.
 */
@Composable
private fun ColumnInfluence(
    column: ColumnDto,
    modifier: Modifier = Modifier,
) {
    val agree = column.authorAgree
    val like = column.authorLike
    val favourite = column.authorFavourite
    val words = column.authorInfluenceWords
    val counted = agree != null && like != null && favourite != null

    if (!counted && words.isEmpty()) return

    Column(modifier = modifier.padding(top = 24.dp)) {
        Text(
            text = stringResource(R.string.column_author_influence),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.SemiBold,
        )

        if (agree != null && like != null && favourite != null) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatItem(stringResource(R.string.column_author_agree), agree)
                StatItem(stringResource(R.string.column_author_like), like)
                StatItem(stringResource(R.string.column_author_favourite), favourite)
            }
        } else {
            Text(
                text = words.joinToString(" · "),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
