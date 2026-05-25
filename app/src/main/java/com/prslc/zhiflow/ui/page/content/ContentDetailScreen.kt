package com.prslc.zhiflow.ui.page.content

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ZhihuAnswer
import com.prslc.zhiflow.data.model.ZhihuContent
import com.prslc.zhiflow.parser.model.RichTextElement
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.widget.CollectionDialog
import com.prslc.zhiflow.ui.component.widget.ImageLightbox
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.navigation.Navigator
import com.prslc.zhiflow.ui.page.comment.CommentBottomSheet
import com.prslc.zhiflow.ui.page.comment.CommentViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDetailScreen(
    id: String,
    contentType: ContentType,
    onBack: () -> Unit,
    viewModel: ContentViewModel = koinViewModel(),
    commentViewModel: CommentViewModel = koinViewModel()
) {
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
            commentViewModel.loadComments(id, contentType)
        }
    }

    LaunchedEffect(id) {
        viewModel.loadContent(id, contentType)
    }

    BackHandler(enabled = presentation.isLightboxVisible) {
        viewModel.dismissLightbox()
    }

    DisposableEffect(id) {
        viewModel.flushProgress(id, contentType)
        onDispose {
            viewModel.flushProgress(id, contentType)
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

    val onVoteClick = remember {
        { action: String -> viewModel.vote(action, contentType) }
    }
    val onStarClick = remember { { viewModel.openCollection() } }
    val onCommentClick = remember { { viewModel.openComments() } }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    ContentDetailTopBar(
                        currentContent = currentContent,
                        isLoading = loadingState.isLoading,
                        contentType = contentType,
                        scrollBehavior = scrollBehavior,
                        onBack = onBack,
                        navigator = navigator
                    )
                },
                bottomBar = {
                    ContentDetailBottomBar(
                        isVisible = isBottomBarVisible && currentContent != null,
                        currentContent = currentContent,
                        interaction = interaction,
                        displayUpvoteCount = viewModel.displayUpvoteCount,
                        onVoteClick = onVoteClick,
                        onStarClick = onStarClick,
                        onCommentClick = onCommentClick
                    )
                }
            ) { padding ->
                when {
                    loadingState.isLoading && currentContent == null -> {
                        LoadingView(modifier = Modifier.fillMaxSize())
                    }

                    loadingState.error != null && currentContent == null -> {
                        ErrorView(
                            message = loadingState.error.uiMessage,
                            onRetry = { viewModel.loadContent(id, contentType) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        currentContent?.let { answer ->
                            key(id) {
                                ContentRichTextList(
                                    id = id,
                                    richTextElements = richTextElements,
                                    answer = answer,
                                    navigator = navigator,
                                    topPadding = padding.calculateTopPadding(),
                                    onImageClick = { url ->
                                        val index = imageUrls.indexOf(url)
                                        if (index != -1) {
                                            viewModel.openLightbox(index)
                                        }
                                    },
                                    onProgress = { viewModel.trackProgress(it) }
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
                viewModel = commentViewModel,
                showComments = presentation.showComments,
                onDismissRequest = {
                    viewModel.dismissComments()
                    commentViewModel.onSheetDismissed()
                }
            )

            if (presentation.isLightboxVisible) {
                ImageLightbox(
                    imageUrls = imageUrls,
                    initialIndex = presentation.currentImageIndex,
                    onDismiss = { viewModel.dismissLightbox() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentDetailTopBar(
    currentContent: ZhihuContent?,
    isLoading: Boolean,
    contentType: ContentType,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit,
    navigator: Navigator
) {
    LargeTopAppBar(
        title = {
            val titleText = when {
                currentContent != null -> currentContent.displayTitle
                isLoading -> ""
                else -> stringResource(R.string.question_title_filed)
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
                    overflow = TextOverflow.Ellipsis
                )
            }
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
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    )
}
