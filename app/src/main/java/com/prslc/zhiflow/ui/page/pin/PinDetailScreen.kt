package com.prslc.zhiflow.ui.page.pin

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.data.model.content.ZhihuPin
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.richtext.RichTextSingleElement
import com.prslc.zhiflow.ui.component.widget.BottomBar
import com.prslc.zhiflow.ui.component.widget.CollectionDialog
import com.prslc.zhiflow.ui.component.widget.ImageLightbox
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.page.comment.CommentBottomSheet
import com.prslc.zhiflow.ui.page.comment.CommentUiEvent
import com.prslc.zhiflow.ui.page.comment.CommentViewModel
import com.prslc.zhiflow.ui.page.content.AuthorSection
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    val presentation = viewModel.presentation
    val currentContent = loadingState.content
    val commentState = commentViewModel.uiState

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val imageUrls = remember(richTextElements) {
        richTextElements
            .filterIsInstance<RichTextElement.Image>()
            .mapNotNull { it.data.urls.firstOrNull() }
    }

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

    DisposableEffect(id) {
        viewModel.flushProgress(id)
        onDispose {
            viewModel.flushProgress(id)
        }
    }

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
                topBar = {
                    if (pinTitle.isNotEmpty()) {
                        LargeTopAppBar(
                            title = {
                                val isCollapsed = scrollBehavior.state.collapsedFraction > 0.5f
                                Text(
                                    text = pinTitle,
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
                    } else {
                        TopAppBar(
                            title = {},
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.general_back),
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                            ),
                        )
                    }
                },
                bottomBar = {
                    if (currentContent != null) {
                        BottomBar(
                            isUpvoted = interaction.isUpvoted,
                            isDownvoted = interaction.isDownvoted,
                            isFavorite = interaction.isFavorite,
                            upvoteCount = viewModel.displayUpvoteCount,
                            favCount = currentContent.reaction.statistics.favoritesCount,
                            commentCount = currentContent.reaction.statistics.commentCount,
                            onVoteClick = onVoteClick,
                            onStarClick = onStarClick,
                            onCommentClick = onCommentClick,
                        )
                    }
                }
            ) { padding ->
                when {
                    loadingState.isLoading && currentContent == null -> {
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
                                PinContentList(
                                    pin = pin,
                                    richTextElements = richTextElements,
                                    navigator = navigator,
                                    topPadding = padding.calculateTopPadding(),
                                    onImageClick = { url ->
                                        val index = imageUrls.indexOf(url)
                                        if (index != -1) {
                                            viewModel.openLightbox(index)
                                        }
                                    },
                                    onProgress = { viewModel.trackProgress(it) },
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
                onDismissRequest = {
                    viewModel.dismissComments()
                    commentViewModel.onSheetDismissed()
                },
                onEvent = { event ->
                    when (event) {
                        CommentUiEvent.DismissSheet -> commentViewModel.onSheetDismissed()
                        CommentUiEvent.BackToMain -> commentViewModel.backToMain()
                        CommentUiEvent.CloseImage -> commentViewModel.closeImage()
                        CommentUiEvent.LoadMoreReplies -> commentViewModel.loadMoreReplies()

                        is CommentUiEvent.LoadRootComments -> commentViewModel.loadComments(event.id, event.contentType)
                        is CommentUiEvent.NavigatedToUser -> commentViewModel.onNavigated()
                        is CommentUiEvent.ToggleLike -> commentViewModel.toggleLike(event.commentId)
                        is CommentUiEvent.OpenImage -> commentViewModel.openImage(event.url)
                        is CommentUiEvent.ShowAuthor -> commentViewModel.showAuthor(event.urlToken)
                        is CommentUiEvent.LoadChildComments -> commentViewModel.loadChildComments(event.rootComment, forceRefresh = true)
                    }
                }
            )

            if (presentation.isLightboxVisible) {
                ImageLightbox(
                    imageUrls = imageUrls,
                    initialIndex = presentation.currentImageIndex,
                    onDismiss = { viewModel.dismissLightbox() },
                )
            }
        }
    }
}

@Composable
private fun PinContentList(
    pin: ZhihuPin,
    richTextElements: List<RichTextElement>,
    navigator: com.prslc.zhiflow.ui.navigation.Navigator,
    topPadding: androidx.compose.ui.unit.Dp,
    onImageClick: (String) -> Unit,
    onProgress: (Int) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    LaunchedEffect(pin.id) {
        snapshotFlow {
            val layout = lazyListState.layoutInfo
            val total = layout.totalItemsCount
            if (total <= 0) 0
            else {
                val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
                ((lastVisible + 1).toFloat() / total * 100).toInt().coerceIn(0, 100)
            }
        }.collect { progress ->
            onProgress(progress)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            AuthorSection(
                author = pin.author,
                navigator = navigator,
            )
        }

        itemsIndexed(
            items = richTextElements,
            key = { index, element ->
                when (element) {
                    is RichTextElement.Divider -> "divider_$index"
                    is RichTextElement.Image -> "img_${element.data.urls.firstOrNull()}_$index"
                    else -> "content_${element::class.simpleName}_$index"
                }
            },
            contentType = { _, element -> element::class.simpleName }
        ) { _, element ->
            Box(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                RichTextSingleElement(
                    element = element,
                    onImageClick = onImageClick,
                )
            }
        }

        item {
            pin.contentEnd?.let { contentEnd ->
                val timeDisplay = contentEnd.updateTime?.takeIf { it.isNotBlank() }
                    ?: contentEnd.createTime?.takeIf { it.isNotBlank() }

                if (!timeDisplay.isNullOrBlank()) {
                    Box(modifier = Modifier.padding(20.dp)) {
                        val text = if (contentEnd.ipInfo.isNotEmpty()) {
                            stringResource(
                                R.string.content_published_with_ip,
                                contentEnd.ipInfo,
                                timeDisplay
                            )
                        } else {
                            stringResource(
                                R.string.content_published_no_ip,
                                timeDisplay
                            )
                        }

                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}
