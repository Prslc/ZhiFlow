package com.prslc.zhiflow.ui.screen

import com.prslc.zhiflow.ui.viewmodel.ContentViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.exception.uiMessage
import com.prslc.zhiflow.data.model.AnswerAuthor
import com.prslc.zhiflow.ui.component.CollectionDialog
import com.prslc.zhiflow.ui.component.ImageLightbox
import com.prslc.zhiflow.ui.component.RichText
import com.prslc.zhiflow.ui.component.comment.CommentBottomSheet
import com.prslc.zhiflow.ui.component.common.BottomBar
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.viewmodel.CommentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDetailScreen(
    id: String,
    contentType: String,
    onBack: () -> Unit,
    viewModel: ContentViewModel = viewModel(),
    commentViewModel: CommentViewModel = viewModel()
) {
    val commentState = commentViewModel.uiState

    var showCollectionSheet by rememberSaveable { mutableStateOf(false) }
    var showComments by remember { mutableStateOf(false) }

    LaunchedEffect(showComments) {
        if (showComments && commentState.comments.isEmpty()) {
            commentViewModel.loadComments(id)
        }
    }

    val currentContent = viewModel.content
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var selectedImageUrl by rememberSaveable { mutableStateOf<String?>(null) }

    val lazyListState = rememberLazyListState()

    BackHandler(enabled = selectedImageUrl != null || currentContent != null) {
        if (selectedImageUrl != null) {
            selectedImageUrl = null
        } else {
            onBack()
        }
    }

    LaunchedEffect(id) {
        viewModel.loadContent(id, contentType)
    }

    var currentProgress by remember { mutableIntStateOf(0) }

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

    DisposableEffect(id) {
        viewModel.updateReadProgress(id, "answer", 0)

        onDispose {
            if (currentProgress > 0) {
                viewModel.updateReadProgress(id, "answer", currentProgress)
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
        // content
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    LargeTopAppBar(
                        title = {
                            val titleText = when {
                                currentContent != null -> currentContent.displayTitle
                                viewModel.isLoading -> ""
                                else -> stringResource(R.string.question_title_filed)
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
                        visible = isBottomBarVisible && currentContent != null,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                    ) {
                        currentContent?.let {
                            BottomBar(
                                isUpvoted = viewModel.isUpvoted,
                                isDownvoted = viewModel.isDownvoted,
                                isFaved = viewModel.isFaved,
                                upvoteCount = viewModel.displayUpvoteCount,
                                favCount = viewModel.content?.reaction?.statistics?.favoritesCount ?: 0,
                                commentCount = viewModel.content?.reaction?.statistics?.commentCount ?: 0,
                                onVoteClick = { type -> viewModel.updateVote(type) },
                                onStarClick = { showCollectionSheet = true },
                                onCommentClick = { showComments = true }
                            )
                        }
                    }
                }) { padding ->
                when {
                    viewModel.isLoading && currentContent == null -> {
                        LoadingView(modifier = Modifier.fillMaxSize())
                    }

                    viewModel.error != null && currentContent == null -> {
                        ErrorView(
                            message = viewModel.error!!.uiMessage,
                            onRetry = { viewModel.loadContent(id, contentType) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        currentContent?.let { answer ->
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
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(
                                            alpha = 0.5f
                                        )
                                    )
                                }

                                // Content
                                item {
                                    Box(
                                        modifier = Modifier.padding(
                                            horizontal = 20.dp, vertical = 16.dp
                                        )
                                    ) {
                                        RichText(
                                            segments = answer.structuredContent.segments,
                                            onImageClick = { url ->
                                                selectedImageUrl = url
                                            })
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
                }
            }

            if (showCollectionSheet) {
                CollectionDialog(
                    id = id,
                    contentType = contentType,
                    onDismissRequest = { showCollectionSheet = false },
                    onResult = { isFavedNow ->
                        viewModel.isFaved = isFavedNow
                    })
            }

            CommentBottomSheet(
                id = id,
                viewModel = commentViewModel,
                showComments = showComments,
                onDismissRequest = {
                    showComments = false
                    commentViewModel.resetState()
                })

            // light box
            ImageLightbox(
                imageUrl = selectedImageUrl, onDismiss = { selectedImageUrl = null })
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