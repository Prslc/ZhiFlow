package com.prslc.zhiflow.ui.page.content

import androidx.compose.animation.core.animate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.FlushProgressOnLeave
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.model.content.ZhihuAnswer
import com.prslc.zhiflow.data.model.content.ZhihuContent
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.rememberActionErrorHost
import com.prslc.zhiflow.ui.component.richtext.BodyOutlineSheet
import com.prslc.zhiflow.ui.component.richtext.ContentBodyList
import com.prslc.zhiflow.ui.component.richtext.rememberContentBodyState
import com.prslc.zhiflow.ui.component.widget.CollectionDialog
import com.prslc.zhiflow.ui.component.widget.ReadingProgressBar
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.navigation.Navigator
import com.prslc.zhiflow.ui.page.comment.CommentBottomSheet
import com.prslc.zhiflow.ui.page.comment.CommentUiEvent
import com.prslc.zhiflow.ui.page.comment.CommentViewModel
import com.prslc.zhiflow.ui.page.comment.SegmentPanelSheet
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDetailScreen(
    id: String,
    contentType: ContentType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContentViewModel = koinViewModel(),
    commentViewModel: CommentViewModel = koinViewModel()
) {
    val uiState = commentViewModel.uiState
    val childUiState = commentViewModel.childUiState

    val navigator = LocalNavigator.current
    val loadingState = viewModel.loadingState
    val interaction = viewModel.interactionState
    val richTextElements = viewModel.richTextElements
    val bodyState = rememberContentBodyState(richTextElements)
    val presentation = viewModel.presentation
    val currentContent = loadingState.content
    val commentState = commentViewModel.uiState

    val snackbarHostState = rememberActionErrorHost(
        viewModel.actionError,
        viewModel::consumeActionError,
    )

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val isDark = isSystemInDarkTheme()

    LaunchedEffect(isDark) {
        viewModel.setDarkMode(isDark)
    }

    LaunchedEffect(presentation.showComments) {
        if (presentation.showComments && commentState.comments.isEmpty()) {
            commentViewModel.loadComments(id, contentType)
        }
    }

    LaunchedEffect(id) {
        viewModel.loadContent(id, contentType)
    }

    FlushProgressOnLeave { viewModel.flushProgress(id, contentType) }

    var isBottomBarVisible by remember { mutableStateOf(true) }
    var showOutline by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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

    val onVoteClick = remember {
        { action: String -> viewModel.vote(action, contentType) }
    }
    val onStarClick = remember { { viewModel.openCollection() } }
    val onCommentClick = remember { { viewModel.openComments() } }

    val segmentKey = viewModel.openSegmentKey
    val segmentTarget = segmentKey?.let { viewModel.segmentLikes[it] }
    val segmentLikes = viewModel.segmentLikes
    val onSegmentLikeClick = remember { { key: String -> viewModel.openSegmentPanel(key) } }

    LaunchedEffect(segmentTarget?.segId) {
        segmentTarget?.segId?.let { segmentId ->
            commentViewModel.loadSegmentComments(id, contentType, segmentId)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    Column {
                        ContentDetailTopBar(
                            currentContent = currentContent,
                            error = loadingState.error,
                            contentType = contentType,
                            scrollBehavior = scrollBehavior,
                            showOutlineAction = bodyState.outline.size >= 2,
                            onOutlineClick = { showOutline = true },
                            onBack = onBack,
                            navigator = navigator,
                        )
                        ReadingProgressBar(progress = { viewModel.readProgress })
                    }
                },
                bottomBar = {
                    // Outside the animated wrapper: a failure drops the bar, never retracts it.
                    if (loadingState.error == null) {
                        ContentDetailBottomBar(
                            isVisible = isBottomBarVisible,
                            currentContent = currentContent,
                            interaction = interaction,
                            upvoteCount = viewModel.displayUpvoteCount,
                            onVoteClick = onVoteClick,
                            onStarClick = onStarClick,
                            onCommentClick = onCommentClick,
                        )
                    }
                }
            ) { padding ->
                when {
                    currentContent == null && loadingState.error == null -> {
                        LoadingView(modifier = Modifier.fillMaxSize())
                    }

                    loadingState.error != null && currentContent == null -> {
                        ErrorView(
                            message = loadingState.error.uiMessage,
                            onRetry = { viewModel.loadContent(id, contentType) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    else -> {
                        currentContent?.let { answer ->
                            key(id) {
                                ContentBodyList(
                                    content = answer,
                                    elements = richTextElements,
                                    state = bodyState,
                                    navigator = navigator,
                                    topPadding = { padding.calculateTopPadding() },
                                    bodyComplete = viewModel.isBodyComplete,
                                    showAuthorDivider = true,
                                    onProgress = { viewModel.trackProgress(it) },
                                    onFollowClick = viewModel::toggleFollow,
                                    segmentLikes = segmentLikes,
                                    onSegmentLikeClick = onSegmentLikeClick,
                                )
                            }
                        }
                    }
                }
            }

            if (presentation.showCollectionSheet) {
                CollectionDialog(
                    id = id,
                    contentType = contentType,
                    onDismissRequest = { viewModel.dismissCollection() },
                    onResult = { isFavedNow ->
                        viewModel.setFaved(isFavedNow)
                        viewModel.dismissCollection()
                    }
                )
            }

            CommentBottomSheet(
                id = id,
                contentType = contentType,
                uiState = uiState,
                childUiState = childUiState,
                showComments = presentation.showComments,
                onDismissRequest = {
                    viewModel.dismissComments()
                    commentViewModel.onSheetDismissed()
                },

                onEvent = { event ->
                    when (event) {
                        CommentUiEvent.DismissSheet -> commentViewModel.onSheetDismissed()
                        CommentUiEvent.BackToMain -> commentViewModel.backToMain()
                        CommentUiEvent.LoadMoreReplies -> commentViewModel.loadMoreReplies()

                        is CommentUiEvent.LoadRootComments -> commentViewModel.loadComments(event.id, event.contentType)
                        is CommentUiEvent.NavigatedToUser -> commentViewModel.onNavigated()
                        is CommentUiEvent.ActionErrorShown -> commentViewModel.onActionErrorShown()
                        is CommentUiEvent.ToggleLike -> commentViewModel.toggleLike(event.commentId)
                        is CommentUiEvent.ShowAuthor -> commentViewModel.showAuthor(event.urlToken)
                        is CommentUiEvent.LoadChildComments -> commentViewModel.loadChildComments(event.rootComment, forceRefresh = true)
                    }
                }
            )

            SegmentPanelSheet(
                target = segmentTarget,
                comments = commentViewModel.segmentUiState,
                childComments = commentViewModel.childUiState,
                navigateToUser = commentViewModel.uiState.navigateToUser,
                onNavigated = commentViewModel::onNavigated,
                actionError = viewModel.actionError ?: commentViewModel.uiState.actionError,
                onErrorConsumed = {
                    viewModel.consumeActionError()
                    commentViewModel.onActionErrorShown()
                },
                onToggleLike = { key -> viewModel.toggleSegmentLike(key) },
                onLoadMore = {
                    segmentTarget?.segId?.let {
                        commentViewModel.loadSegmentComments(id, contentType, it)
                    }
                },
                onRetry = {
                    segmentTarget?.segId?.let {
                        commentViewModel.loadSegmentComments(id, contentType, it, forceRefresh = true)
                    }
                },
                onDismissRequest = {
                    viewModel.dismissSegmentPanel()
                    commentViewModel.onSheetDismissed()
                },
                onEvent = { event ->
                    when (event) {
                        is CommentUiEvent.ToggleLike -> commentViewModel.toggleLike(event.commentId)
                        is CommentUiEvent.ShowAuthor -> commentViewModel.showAuthor(event.urlToken)
                        is CommentUiEvent.LoadChildComments -> commentViewModel.loadChildComments(
                            event.rootComment,
                            forceRefresh = true
                        )

                        CommentUiEvent.LoadMoreReplies -> commentViewModel.loadMoreReplies()
                        CommentUiEvent.BackToMain -> commentViewModel.backToMain()
                        CommentUiEvent.ActionErrorShown -> commentViewModel.onActionErrorShown()
                        else -> Unit
                    }
                },
            )

            BodyOutlineSheet(
                visible = showOutline,
                entries = bodyState.outline,
                currentIndex = { bodyState.currentOutlineIndex },
                onEntryClick = { entry ->
                    showOutline = false
                    scope.launch {
                        // Landing on a section leaves the bar out of the way. It has to be moved
                        // here: a far jump never reaches the bar's own scroll connection.
                        launch { scrollBehavior.retract() }
                        bodyState.scrollTo(entry)
                    }
                },
                onDismissRequest = { showOutline = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentDetailTopBar(
    currentContent: ZhihuContent?,
    error: ApiException?,
    contentType: ContentType,
    scrollBehavior: TopAppBarScrollBehavior,
    showOutlineAction: Boolean,
    onOutlineClick: () -> Unit,
    onBack: () -> Unit,
    navigator: Navigator
) {
    LargeTopAppBar(
        title = {
            val titleText = when {
                currentContent != null -> currentContent.displayTitle
                error == null -> ""
                else -> stringResource(R.string.content_title_filed)
            }

            val isCollapsed = scrollBehavior.state.collapsedFraction > 0.5f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (contentType == ContentType.ANSWER && currentContent is ZhihuAnswer) {
                            Modifier.clickable(
                                onClick = {
                                    currentContent.question?.id?.let { qId ->
                                        navigator.navigateToContent(qId, "question")
                                    }
                                }
                            )
                        } else Modifier
                    )
                    .padding(end = 10.dp)
            ) {
                Text(
                    text = titleText,
                    modifier = Modifier.padding(end = 10.dp),
                    style = if (isCollapsed) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = if (isCollapsed) 1 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.general_back),
                )
            }
        },
        actions = {
            if (showOutlineAction) {
                IconButton(onClick = onOutlineClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.outline_title),
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        )
    )
}

/** Puts the bar out of the way, the way arriving at a section should leave it. */
private suspend fun TopAppBarScrollBehavior.retract() {
    animate(
        initialValue = state.heightOffset,
        targetValue = state.heightOffsetLimit,
    ) { value, _ -> state.heightOffset = value }
}
