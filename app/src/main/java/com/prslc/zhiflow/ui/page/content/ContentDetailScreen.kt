package com.prslc.zhiflow.ui.page.content

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.BarCollapseDistance
import com.prslc.zhiflow.core.utils.compose.FlushProgressOnLeave
import com.prslc.zhiflow.core.utils.compose.rememberHideOnScroll
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
import com.prslc.zhiflow.ui.page.comment.CommentBottomSheet
import com.prslc.zhiflow.ui.page.comment.CommentUiEvent
import com.prslc.zhiflow.ui.page.comment.CommentViewModel
import com.prslc.zhiflow.ui.page.comment.SegmentPanelSheet
import com.prslc.zhiflow.ui.page.comment.canCommentOn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun ContentDetailScreen(
    id: String,
    contentType: ContentType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContentViewModel = koinViewModel(),
    commentViewModel: CommentViewModel = koinViewModel(),
) {
    val uiState = commentViewModel.uiState
    val childUiState = commentViewModel.childUiState

    val navigator = LocalNavigator.current
    val loadingState = viewModel.loadingState
    val interaction = viewModel.interactionState
    val bodyState by rememberContentBodyState { viewModel.richTextElements }
    val presentation = viewModel.presentation
    val currentContent = loadingState.content
    val commentState = commentViewModel.uiState

    // What the content and the comment section say between them about writing here.
    val canComment = commentState.canCommentOn(currentContent)

    val snackbarHostState = rememberActionErrorHost(
        viewModel.actionError,
        viewModel::consumeActionError,
    )

    // One bar, one row, whatever the page: it goes with the finger and never folds. Material's
    // large bar cross-fades the title between two rows, which with a question's title reads as one
    // title being replaced by another rather than as a title folding away.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val topBarLeave = rememberTopBarLeave { true }
    // The status bar is the system's: the page paints its pad and never shortens it, so the bar
    // leaves the row and the content stays clear of the icons.
    val statusBarPadPx = WindowInsets.statusBars.getTop(LocalDensity.current)

    // Opening the sheet is a visit of its own: the list is read again rather than resumed, however
    // recently it was last seen.
    LaunchedEffect(presentation.showComments) {
        if (presentation.showComments) {
            commentViewModel.loadComments(id, contentType, forceRefresh = true)
        }
    }

    LaunchedEffect(id) {
        viewModel.loadContent(id, contentType)
    }

    FlushProgressOnLeave { viewModel.flushProgress(id, contentType) }

    var showOutline by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // The bottom bar goes with the finger, as the bar at the foot of the main screen does.
    val bottomBarHide = rememberHideOnScroll(BarCollapseDistance)

    val onVoteClick = remember {
        { action: String -> viewModel.vote(action, contentType) }
    }
    val onStarClick = remember { { viewModel.openCollection() } }
    val onCommentClick = remember { { viewModel.openComments() } }

    // What the bar's title opens, where it opens anything: an answer's title leads back to its
    // question.
    val openQuestion: (() -> Unit)? =
        (currentContent as? ZhihuAnswer)
            ?.takeIf { contentType == ContentType.ANSWER }
            ?.question
            ?.id
            ?.let { questionId -> { navigator.navigateToContent(questionId, "question") } }

    // The bar carries the page's title only once the body's own copy has been read past: there is
    // one title on the screen at a time, and the bar is not an empty row once the page is read.
    val headerReadPast by remember(bodyState.listState) {
        derivedStateOf {
            (bodyState.listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0) > 0
        }
    }

    val segmentKey = viewModel.openSegmentKey
    val segmentTarget = segmentKey?.let { viewModel.segmentLikes[it] }
    val segmentLikes = viewModel.segmentLikes
    val onSegmentLikeClick = remember { { key: String -> viewModel.openSegmentPanel(key) } }

    // Opening the panel is a visit of its own, as the comment sheet's is.
    LaunchedEffect(segmentTarget?.segId) {
        segmentTarget?.segId?.let { segmentId ->
            commentViewModel.loadSegmentComments(id, contentType, segmentId, forceRefresh = true)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(bottomBarHide.connection),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = Modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    // Nearer than the behaviour, so this stage meets a delta first: going down it
                    // stands aside until the title has collapsed; coming up the bar is home before
                    // the title has any of the scroll.
                    .nestedScroll(topBarLeave.connection),
                topBar = {
                    Column {
                        TopBarStrip(scrollBehavior, topBarLeave, statusBarPadPx)
                        Box(modifier = Modifier.retractedBy(topBarLeave)) {
                            ContentTopBar(
                                title = currentContent?.displayTitle.orEmpty(),
                                // A pin's title is only ever in the bar: its page has no body
                                // header to carry it.
                                titleVisible = contentType == ContentType.PIN || headerReadPast,
                                onTitleClick = openQuestion,
                                scrollBehavior = scrollBehavior,
                                onBack = onBack,
                                showOutlineAction = bodyState.outline.size >= 2,
                                onOutlineClick = { showOutline = true },
                            )
                        }
                        ReadingProgressBar(progress = { viewModel.readProgress })
                    }
                },
                bottomBar = {
                    // Outside the animated wrapper: a failure drops the bar, never retracts it.
                    if (loadingState.error == null) {
                        ContentDetailBottomBar(
                            hide = bottomBarHide,
                            currentContent = currentContent,
                            interaction = interaction,
                            upvoteCount = viewModel.displayUpvoteCount,
                            onVoteClick = onVoteClick,
                            onStarClick = onStarClick,
                            onCommentClick = onCommentClick,
                        )
                    }
                },
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
                                    elements = viewModel.richTextElements,
                                    state = bodyState,
                                    navigator = navigator,
                                    header = {
                                        ContentDetailHeader(
                                            contentType = contentType,
                                            content = currentContent,
                                            onTitleClick = openQuestion,
                                        )
                                    },
                                    topPadding = { padding.calculateTopPadding() },
                                    bodyComplete = viewModel.isBodyComplete,
                                    showAuthorDivider = contentType != ContentType.PIN,
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
                    },
                )
            }

            CommentBottomSheet(
                id = id,
                contentType = contentType,
                uiState = uiState,
                childUiState = childUiState,
                showComments = presentation.showComments,
                canComment = canComment,
                // Hides the sheet only: the sheet clears the comment state itself once it is gone.
                onDismissRequest = { viewModel.dismissComments() },
                onEvent = { event ->
                    when (event) {
                        CommentUiEvent.DismissSheet -> commentViewModel.onSheetDismissed()
                        CommentUiEvent.BackToMain -> commentViewModel.backToMain()
                        CommentUiEvent.LoadMoreReplies -> commentViewModel.loadMoreReplies()
                        is CommentUiEvent.LoadRootComments -> commentViewModel.loadComments(event.id, event.contentType)
                        is CommentUiEvent.NavigatedToUser -> commentViewModel.onNavigated()
                        is CommentUiEvent.ActionErrorShown -> commentViewModel.consumeActionError()
                        is CommentUiEvent.ToggleLike -> commentViewModel.toggleLike(event.commentId)
                        is CommentUiEvent.ToggleDislike -> commentViewModel.toggleDislike(event.commentId)
                        is CommentUiEvent.ShowAuthor -> commentViewModel.showAuthor(event.urlToken)
                        is CommentUiEvent.LoadChildComments -> commentViewModel.loadChildComments(event.rootComment, forceRefresh = true)
                        is CommentUiEvent.ReplyTo -> commentViewModel.replyTo(event.comment)
                        CommentUiEvent.OpenComposer -> commentViewModel.openComposer()
                        CommentUiEvent.CloseComposer -> commentViewModel.closeComposer()
                        is CommentUiEvent.DraftChanged -> commentViewModel.onDraftChange(event.text)
                        is CommentUiEvent.SubmitComment -> commentViewModel.submitComment(event.id, event.contentType)
                        is CommentUiEvent.SubmitSegmentComment -> commentViewModel.submitSegmentComment(event.id, event.contentType, event.anchor)
                    }
                },
            )

            SegmentPanelSheet(
                id = id,
                contentType = contentType,
                target = segmentTarget,
                comments = commentViewModel.segmentUiState,
                childComments = commentViewModel.childUiState,
                draft = commentViewModel.uiState.draft,
                isSubmitting = commentViewModel.uiState.isSubmitting,
                isComposerOpen = commentViewModel.uiState.isComposerOpen,
                canComment = canComment,
                navigateToUser = commentViewModel.uiState.navigateToUser,
                onNavigated = commentViewModel::onNavigated,
                actionError = viewModel.actionError ?: commentViewModel.uiState.actionError,
                onErrorConsumed = {
                    viewModel.consumeActionError()
                    commentViewModel.consumeActionError()
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

                        is CommentUiEvent.ToggleDislike -> commentViewModel.toggleDislike(event.commentId)

                        is CommentUiEvent.ShowAuthor -> commentViewModel.showAuthor(event.urlToken)

                        is CommentUiEvent.LoadChildComments -> commentViewModel.loadChildComments(
                            event.rootComment,
                            forceRefresh = true,
                        )

                        is CommentUiEvent.ReplyTo -> commentViewModel.replyTo(event.comment)

                        CommentUiEvent.OpenComposer -> commentViewModel.openComposer()

                        CommentUiEvent.CloseComposer -> commentViewModel.closeComposer()

                        is CommentUiEvent.DraftChanged -> commentViewModel.onDraftChange(event.text)

                        is CommentUiEvent.SubmitSegmentComment -> commentViewModel.submitSegmentComment(
                            event.id,
                            event.contentType,
                            event.anchor,
                        )

                        CommentUiEvent.LoadMoreReplies -> commentViewModel.loadMoreReplies()

                        CommentUiEvent.BackToMain -> commentViewModel.backToMain()

                        CommentUiEvent.ActionErrorShown -> commentViewModel.consumeActionError()

                        else -> Unit
                    }
                },
            )

            BodyOutlineSheet(
                visible = showOutline,
                entries = { bodyState.outline },
                currentIndex = { bodyState.currentOutlineIndex },
                onEntryClick = { entry ->
                    showOutline = false
                    scope.launch { bodyState.scrollTo(entry) }
                },
                onDismissRequest = { showOutline = false },
            )
        }
    }
}

/**
 * The page's bar: the way back, what the page is about once the body's own copy is out of sight,
 * and the outline where the body has one.
 *
 * One row for every page: Material's large bar draws its title in two rows and cross-fades them,
 * which with a question's title reads as one title being replaced by another rather than as a title
 * folding away. The body carries the title instead, and this row picks it up once it is gone.
 *
 * @param titleVisible Whether the body's title is out of view; the bar's copy fades in then.
 * @param onTitleClick What the title opens, where it opens anything: an answer's title leads back
 *   to its question.
 */
@Composable
private fun ContentTopBar(
    title: String,
    titleVisible: Boolean,
    onTitleClick: (() -> Unit)?,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit,
    showOutlineAction: Boolean,
    onOutlineClick: () -> Unit,
) {
    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = tween(150),
        label = "contentBarTitle",
    )

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .alpha(titleAlpha)
                    .then(
                        if (onTitleClick != null) {
                            Modifier.clickable(onClick = onTitleClick)
                        } else {
                            Modifier
                        },
                    )
                    .padding(end = 10.dp),
            )
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
        // The pad is drawn above the bar by TopBarStrip, so the bar measures only itself and the
        // pad stays when the row leaves.
        windowInsets = WindowInsets(0),
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        ),
    )
}

/**
 * What the page is about, as its first line: read with the page rather than carried in the bar, so
 * it scrolls away with everything else and the screen never shows the same title twice.
 */
@Composable
private fun ContentDetailHeader(
    contentType: ContentType,
    content: ZhihuContent?,
    onTitleClick: (() -> Unit)?,
) {
    if (contentType == ContentType.PIN || content == null) return

    Text(
        text = content.displayTitle,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    )
}
