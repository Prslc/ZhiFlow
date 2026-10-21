package com.prslc.zhiflow.ui.page.notification

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.shouldLoadMore
import com.prslc.zhiflow.core.utils.compose.sizedImageUrl
import com.prslc.zhiflow.data.dto.NotificationItemDto
import com.prslc.zhiflow.ui.component.common.BadgeLabel
import com.prslc.zhiflow.ui.component.common.EmptyView
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.pagingFooter
import com.prslc.zhiflow.ui.component.common.rememberActionErrorHost
import com.prslc.zhiflow.ui.component.richtext.ZRichText
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface
import com.prslc.zhiflow.ui.preview.sampleNotificationDay
import com.prslc.zhiflow.ui.preview.sampleNotificationEntry
import org.koin.androidx.compose.koinViewModel

/**
 * One list of notifications: comments, votes, follows, or whatever else the server named.
 *
 * A card opens whatever its own link points at, which is not always content this app has a page
 * for: a link it cannot resolve is handed to the browser rather than doing nothing.
 *
 * @param type Which list to read, in the spelling the message page names it by.
 * @param title The server's name for the list, which heads the page.
 * @param onBack Called by the back arrow.
 * @param modifier Applied to the scaffold.
 * @param viewModel The page's own; a caller passes one only to test.
 */
@Composable
fun NotificationCategoryScreen(
    type: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationCategoryViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    val uiState = viewModel.uiState
    val items = uiState.items
    val globalError = uiState.globalError

    LaunchedEffect(type) { viewModel.load(type) }

    val shouldLoadMore by remember { viewModel.listState.shouldLoadMore() }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    val snackbarHostState = rememberActionErrorHost(
        viewModel.actionError,
        viewModel::consumeActionError,
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
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
                actions = {
                    // Only there when there is something to mark: a button that has nothing to do
                    // is one the reader has to find out about by tapping it.
                    if (uiState.hasUnread || uiState.isMarkingRead) {
                        IconButton(
                            onClick = viewModel::markAllRead,
                            enabled = uiState.hasUnread && !uiState.isMarkingRead,
                        ) {
                            Icon(
                                Icons.Filled.DoneAll,
                                stringResource(R.string.notification_mark_all_read),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                items.isEmpty() && uiState.isLoading -> LoadingView(modifier = Modifier.fillMaxSize())

                items.isEmpty() && globalError != null -> ErrorView(
                    message = globalError.uiMessage,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                )

                items.isEmpty() -> EmptyView(
                    message = stringResource(R.string.notification_empty),
                    modifier = Modifier.fillMaxSize(),
                )

                else -> LazyColumn(
                    state = viewModel.listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    itemsIndexed(
                        items = items,
                        key = { _, item -> item.notificationKey },
                        contentType = { _, item ->
                            when (item) {
                                is NotificationItemDto.Day -> "day"
                                is NotificationItemDto.Entry -> "entry"
                            }
                        },
                    ) { index, item ->
                        when (item) {
                            is NotificationItemDto.Day -> DaySeparator(item)

                            is NotificationItemDto.Entry -> NotificationRow(
                                item = item,
                                onClick = { navigator.handleUrl(item.targetLink) },
                            )
                        }

                        val next = items.getOrNull(index + 1)
                        if (item is NotificationItemDto.Entry && next is NotificationItemDto.Entry) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                        }
                    }

                    pagingFooter(
                        isLoading = uiState.isNextLoading,
                        error = uiState.loadMoreError,
                        onRetry = viewModel::loadMore,
                    )
                }
            }
        }
    }
}

/** The line that says which day the cards under it belong to, and how many of them it holds. */
@Composable
private fun DaySeparator(day: NotificationItemDto.Day, modifier: Modifier = Modifier) {
    // Counted where the server says there is a count at all: a day it sends no number for is one
    // it does not want counted, and a day of none is what a deleted notification leaves behind.
    val text = if (day.count > 0) {
        stringResource(R.string.notification_day_count, day.text, day.count)
    } else {
        day.text
    }

    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

/**
 * One notification, in the layers the server fills in: who and when, what they did, what it was
 * done to, and its own line for that thing.
 */
@Composable
private fun NotificationRow(
    item: NotificationItemDto.Entry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AsyncImage(
            model = sizedImageUrl(item.avatarUrl, 44.dp),
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                val label = item.label
                if (label != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    BadgeLabel(
                        text = label,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = relativeDay(item.createdTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            // What was done and what it was done to are one line, as the client's own draws them:
            // the two are joined by a separator, and only a card with no body is left with the
            // first half alone. The body keeps the markup it arrived in, so that a comment's links
            // can be followed and its emoji are drawn as the pictures they are.
            if (item.subTitle.isNotBlank() || item.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))

                val separator = stringResource(R.string.notification_action_separator)
                ZRichText(
                    content = buildAnnotatedString {
                        append(item.subTitle)
                        if (item.subTitle.isNotBlank() && item.text.isNotBlank()) {
                            append(separator)
                        }
                        append(item.text)
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }

            if (item.targetText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.targetText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Its own content gone is drawn as a mark rather than as a line: there is nothing left
            // to read, and the sentence saying so is this app's rather than the server's.
            if (item.isContentDeleted) {
                Spacer(modifier = Modifier.height(6.dp))
                BadgeLabel(
                    text = stringResource(R.string.notification_deleted_content),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // The one mark for an unread card: nothing else on the row says so.
        if (!item.isRead) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@AppPreviews
@Composable
private fun NotificationRowPreview() {
    PreviewSurface {
        Column {
            DaySeparator(day = sampleNotificationDay)
            NotificationRow(item = sampleNotificationEntry, onClick = {})
        }
    }
}
