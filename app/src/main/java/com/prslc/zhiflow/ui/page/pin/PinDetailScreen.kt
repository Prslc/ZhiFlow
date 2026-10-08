package com.prslc.zhiflow.ui.page.pin

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.FlushProgressOnLeave
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.rememberActionErrorHost
import com.prslc.zhiflow.ui.component.richtext.ContentBodyList
import com.prslc.zhiflow.ui.component.richtext.rememberContentBodyState
import com.prslc.zhiflow.ui.component.widget.BottomBar
import com.prslc.zhiflow.ui.component.widget.CollectionDialog
import com.prslc.zhiflow.ui.component.widget.ReadingProgressBar
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.page.comment.CommentBottomSheet
import com.prslc.zhiflow.ui.page.comment.CommentUiEvent
import com.prslc.zhiflow.ui.page.comment.CommentViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PinDetailScreen(
    id: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PinViewModel = koinViewModel(),
    commentViewModel: CommentViewModel = koinViewModel(),
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

    // Pinned: a single-line bar cannot collapse, and a pin's title is one line of the
    // author's own text — a 152dp expanded bar costs a quarter of the screen for it.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val isDark = isSystemInDarkTheme()

    LaunchedEffect(isDark) {
        viewModel.setDarkMode(isDark)
    }

    LaunchedEffect(presentation.showComments) {
        if (presentation.showComments && commentState.comments.isEmpty()) {
            commentViewModel.loadComments(id, com.prslc.zhiflow.data.model.content.ContentType.PIN)
        }
    }

    LaunchedEffect(id) {
        viewModel.loadContent(id)
    }

    FlushProgressOnLeave { viewModel.flushProgress(id) }

    val onVoteClick: (String) -> Unit = { action -> viewModel.vote(action) }
    val onStarClick = { viewModel.openCollection() }
    val onCommentClick = { viewModel.openComments() }

    val pinTitle = currentContent?.header?.text.orEmpty()
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                Text(
                                    text = pinTitle,
                                    modifier = Modifier.padding(end = 10.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
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
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                            ),
                        )
                        ReadingProgressBar(progress = { viewModel.readProgress })
                    }
                },
                bottomBar = {
                    if (loadingState.error == null) {
                        BottomBar(
                            isUpvoted = interaction.isUpvoted,
                            isDownvoted = interaction.isDownvoted,
                            isFavorite = interaction.isFavorite,
                            upvoteCount = viewModel.displayUpvoteCount,
                            favCount = currentContent?.reaction?.statistics?.favoritesCount,
                            commentCount = currentContent?.reaction?.statistics?.commentCount,
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
                            onRetry = { viewModel.loadContent(id) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    else -> {
                        currentContent?.let { pin ->
                            key(id) {
                                ContentBodyList(
                                    content = pin,
                                    elements = richTextElements,
                                    state = bodyState,
                                    navigator = navigator,
                                    topPadding = { padding.calculateTopPadding() },
                                    bodyComplete = viewModel.isBodyComplete,
                                    showAuthorDivider = false,
                                    onProgress = { viewModel.trackProgress(it) },
                                    onFollowClick = viewModel::toggleFollow,
                                )
                            }
                        }
                    }
                }
            }

            if (presentation.showCollectionSheet) {
                CollectionDialog(
                    id = id,
                    contentType = com.prslc.zhiflow.data.model.content.ContentType.PIN,
                    onDismissRequest = { viewModel.dismissCollection() },
                    onResult = { isFavedNow ->
                        viewModel.setFaved(isFavedNow)
                        viewModel.dismissCollection()
                    }
                )
            }

            CommentBottomSheet(
                id = id,
                contentType = com.prslc.zhiflow.data.model.content.ContentType.PIN,
                uiState = uiState,
                childUiState = childUiState,
                showComments = presentation.showComments,
                // Hides the sheet only: the sheet clears the comment state itself once it is gone.
                onDismissRequest = { viewModel.dismissComments() },
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
        }
    }
}
