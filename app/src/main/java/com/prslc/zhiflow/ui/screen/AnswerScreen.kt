package com.prslc.zhiflow.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.exception.uiMessage
import com.prslc.zhiflow.data.model.AnswerAuthor
import com.prslc.zhiflow.ui.component.CollectionDialog
import com.prslc.zhiflow.ui.component.ErrorView
import com.prslc.zhiflow.ui.component.ImageLightbox
import com.prslc.zhiflow.ui.component.RichText
import com.prslc.zhiflow.ui.viewmodel.AnswerViewModel
import com.prslc.zhiflow.utils.formatCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerScreen(
    answerId: String,
    onBack: () -> Unit,
    viewModel: AnswerViewModel = viewModel()
) {
    var showCollectionSheet by rememberSaveable { mutableStateOf(false) }

    val currentAnswer = viewModel.answer
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var selectedImageUrl by rememberSaveable { mutableStateOf<String?>(null) }

    val lazyListState = rememberLazyListState()

    BackHandler(enabled = selectedImageUrl != null || currentAnswer != null) {
        if (selectedImageUrl != null) {
            selectedImageUrl = null
        } else {
            onBack()
        }
    }

    LaunchedEffect(answerId) {
        viewModel.loadAnswer(answerId)
    }

    var currentProgress by remember { mutableStateOf(0) }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val layout = lazyListState.layoutInfo
            val total = layout.totalItemsCount
            if (total <= 0) 0
            else {
                val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
                ((lastVisible + 1).toFloat() / total * 100).toInt().coerceIn(0, 100)
            }
        }.collect { currentProgress = it }
    }

    DisposableEffect(answerId) {
        viewModel.updateReadProgress(answerId, "answer", 0)

        onDispose {
            if (currentProgress > 0) {
                viewModel.updateReadProgress(answerId, "answer", currentProgress)
            }
        }
    }

    var isBottomBarVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -5) {
                    isBottomBarVisible = false
                } else if (available.y > 5) {
                    isBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        color = MaterialTheme.colorScheme.background
    ) {

        // loading
        if (viewModel.isLoading && currentAnswer == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.general_loading),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // error
        if (viewModel.error != null && currentAnswer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                ErrorView(
                    message = viewModel.error!!.uiMessage,
                    onRetry = { viewModel.loadAnswer(answerId) }
                )
            }
        }

        // content
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    LargeTopAppBar(
                        title = {
                            val titleText = when {
                                viewModel.isLoading && currentAnswer == null -> stringResource(R.string.general_loading)
                                viewModel.error != null && currentAnswer == null -> stringResource(R.string.error_load_failed)
                                currentAnswer != null -> {
                                    currentAnswer.question?.title
                                        ?: currentAnswer.header?.text
                                        ?: stringResource(R.string.question_title_filed)
                                }

                                else -> ""
                            }

                            val isCollapsed = scrollBehavior.state.collapsedFraction > 0.5f

                            Text(
                                text = titleText,
                                modifier = Modifier.padding(
                                    end = 10.dp
                                ),
                                style = if (isCollapsed) {
                                    MaterialTheme.typography.titleMedium
                                } else {
                                    MaterialTheme.typography.headlineSmall
                                },
                                fontWeight = FontWeight.Bold,
                                maxLines = if (isCollapsed) 1 else 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.general_back)
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                2.dp
                            )
                        )
                    )
                },
                bottomBar = {
                    AnimatedVisibility(
                        visible = isBottomBarVisible && currentAnswer != null,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                    ) {
                        currentAnswer?.let { answer ->
                            AnswerBottomBar(
                                viewModel = viewModel,
                                onComment = { TODO() },
                                onStar = { showCollectionSheet = true }
                            )
                        }
                    }
                }
            ) { padding ->
                currentAnswer?.let { answer ->
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            // Only use top padding to prevent content jumping when the bottom bar animates.
                            .padding(top = padding.calculateTopPadding()),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // author
                        item {
                            AuthorSection(answer.author)
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }

                        // Content
                        item {
                            Box(
                                modifier = Modifier.padding(
                                    horizontal = 20.dp,
                                    vertical = 16.dp
                                )
                            ) {
                                RichText(
                                    segments = answer.structuredContent.segments,
                                    onImageClick = { url ->
                                        selectedImageUrl = url
                                    }
                                )
                            }
                        }

                        // end
                        item {
                            answer.contentEnd?.let { contentEnd ->
                                Column(modifier = Modifier.padding(20.dp)) {
                                    val timeDisplay = when {
                                        !contentEnd.updateTime.isNullOrBlank() -> contentEnd.updateTime
                                        !contentEnd.createTime.isNullOrBlank() -> contentEnd.createTime
                                        else -> ""
                                    }

                                    Text(
                                        text = stringResource(
                                            R.string.answer_published_format,
                                            contentEnd.ipInfo,
                                            timeDisplay
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (showCollectionSheet) {
                CollectionDialog(
                    answerId = answerId,
                    onDismissRequest = { showCollectionSheet = false },
                    onResult = { isFavedNow ->
                        viewModel.isFaved = isFavedNow
                    }
                )
            }

            // light box
            ImageLightbox(
                imageUrl = selectedImageUrl,
                onDismiss = { selectedImageUrl = null }
            )
        }
    }
}

@Composable
fun AuthorSection(author: AnswerAuthor) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        AsyncImage(
            model = author.avatar?.avatarImage?.day,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = author.fullname,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (author.description.isNotEmpty()) {
                Text(
                    text = author.description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AnswerBottomBar(
    viewModel: AnswerViewModel,
    onComment: () -> Unit = {},
    onStar: () -> Unit = {}
) {
    val isUpvoted = viewModel.isUpvoted
    val isDownvoted = viewModel.isDownvoted

    val upvoteBgColor by animateColorAsState(
        targetValue = if (isUpvoted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = 0.5f
        ),
        label = "upvoteBg"
    )
    val upvoteContentColor by animateColorAsState(
        targetValue =
            if (isUpvoted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "upvoteContent"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1.4f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(upvoteBgColor),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Upvote
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { viewModel.updateVote("up") },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isUpvoted) Icons.Filled.ArrowUpward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = upvoteContentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                R.string.bottom_upvote,
                                formatCount(viewModel.displayUpvoteCount)
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = upvoteContentColor
                        )
                    }
                }

                VerticalDivider(
                    modifier = Modifier.height(18.dp),
                    thickness = 1.dp,
                    color = upvoteContentColor.copy(alpha = 0.15f)
                )

                // downvote
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .fillMaxHeight()
                        .clickable { viewModel.updateVote("down") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isDownvoted) MaterialTheme.colorScheme.error else upvoteContentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // star, comment
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomActionItem(
                    icon = if (viewModel.isFaved) Icons.Filled.Star else Icons.Default.Star,
                    label = formatCount(
                        viewModel.answer?.reaction?.statistics?.favoritesCount ?: 0
                    ),
                    iconTint = if (viewModel.isFaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onStar
                )
                BottomActionItem(
                    icon = Icons.AutoMirrored.Default.Comment,
                    label = formatCount(viewModel.answer?.reaction?.statistics?.commentCount ?: 0),
                    onClick = onComment
                )
            }
        }
    }
}

@Composable
private fun BottomActionItem(
    icon: ImageVector,
    label: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                fontWeight = FontWeight.Medium
            )
        }
    }
}