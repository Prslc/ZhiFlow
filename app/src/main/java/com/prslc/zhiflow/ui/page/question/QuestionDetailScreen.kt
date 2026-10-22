package com.prslc.zhiflow.ui.page.question

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.isOverflowed
import com.prslc.zhiflow.core.utils.compose.sizedImageUrl
import com.prslc.zhiflow.data.dto.QuestionAuthorDto
import com.prslc.zhiflow.data.model.content.Topic
import com.prslc.zhiflow.data.model.content.ZhihuImage
import com.prslc.zhiflow.data.remote.parser.engine.AnnotatedStringBuilder
import com.prslc.zhiflow.data.remote.parser.model.DetailElement
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.QuestionStats
import com.prslc.zhiflow.ui.component.common.pagingFooter
import com.prslc.zhiflow.ui.component.richtext.component.ImageComponent
import com.prslc.zhiflow.ui.component.richtext.withThemeSpans
import com.prslc.zhiflow.ui.component.widget.ImageLightboxController
import com.prslc.zhiflow.ui.component.widget.LightboxItem
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface
import com.prslc.zhiflow.ui.preview.sampleTopics
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun QuestionDetailScreen(
    id: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuestionViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var isExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(id) { viewModel.loadQuestion(id) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            QuestionTopBar(
                state = uiState,
                scrollBehavior = scrollBehavior,
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                uiState.isLoading && uiState.question == null -> {
                    LoadingView(Modifier.fillMaxSize())
                }

                uiState.globalError != null && uiState.question == null -> {
                    ErrorView(
                        message = uiState.globalError.uiMessage,
                        onRetry = { viewModel.loadQuestion(id) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                uiState.question != null -> {
                    QuestionContentList(
                        state = uiState,
                        id = id,
                        isExpanded = isExpanded,
                        onExpandChange = { isExpanded = it },
                        modifier = Modifier.fillMaxSize(),
                        onEvent = { event ->
                            when (event) {
                                is QuestionUiEvent.LoadMore -> viewModel.loadMore(event.id)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionContentList(
    state: QuestionViewModel.QuestionUiState,
    id: String,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onEvent: (QuestionUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val lightbox = koinInject<ImageLightboxController>()
    val onImageClick: (ZhihuImage) -> Unit = { tapped ->
        val lightboxItems = state.elements.filterIsInstance<DetailElement.Image>()
            .map { LightboxItem.Image(it.image) }
        lightbox.open(lightboxItems, tapped)
    }

    val lazyListState = rememberLazyListState()
    var firstItemOverflowed by remember { mutableStateOf(false) }

    val showExpandButton by remember(state.elements.size, isExpanded, firstItemOverflowed) {
        derivedStateOf {
            isExpanded || state.elements.size > 1 || firstItemOverflowed
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisibleItem.index >= lazyListState.layoutInfo.totalItemsCount - 2
        }
    }

    LaunchedEffect(shouldLoadMore, state.answers.size) {
        if (shouldLoadMore && !state.isNextLoading && state.hasMore && state.loadMoreError == null) {
            onEvent(QuestionUiEvent.LoadMore(id))
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // who asked
        state.asker?.let { asker ->
            item(key = "asker") {
                QuestionAuthor(
                    author = asker,
                    onClick = { navigator.navigateToPeople(asker.profileToken) },
                )
            }
        }

        item(key = "element_header") {
            state.elements.firstOrNull()?.let { first ->
                QuestionElement(
                    element = first,
                    isExpanded = isExpanded,
                    onOverflow = { firstItemOverflowed = it },
                    onImageClick = onImageClick,
                    onUrlClick = { url -> navigator.handleUrl(url) },
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .animateContentSize(),
                )
            }
        }

        item(key = "element_remaining") {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    for (index in 1 until state.elements.size) {
                        QuestionElement(
                            element = state.elements[index],
                            isExpanded = true,
                            onOverflow = {},
                            onImageClick = onImageClick,
                            onUrlClick = { url -> navigator.handleUrl(url) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        // toggle expand
        item(key = "description_toggle") {
            if (showExpandButton) {
                ExpandToggleButton(
                    isExpanded = isExpanded,
                    onClick = { onExpandChange(!isExpanded) },
                )
            }
        }

        // topic tab
        state.question?.let { question ->
            if (question.topics.isNotEmpty()) {
                item(key = "topics_row") {
                    TopicRow(topics = question.topics)
                }
            }
        }

        // stats
        state.question?.let { question ->
            item(key = "stats_section") {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    QuestionStats(
                        answerCount = question.answerCount,
                        followerCount = question.followerCount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                    HorizontalDivider(
                        thickness = 8.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }

        // question list
        itemsIndexed(
            items = state.answers,
            key = { _, item -> item.id },
        ) { _, display ->
            AnswerItem(
                display = display,
                onClick = { answerId ->
                    navigator.navigateToContent(answerId, "answer")
                },
            )
            AnswerDivider()
        }

        pagingFooter(
            keyPrefix = "answers",
            isLoading = state.isNextLoading,
            error = state.loadMoreError,
            onRetry = { onEvent(QuestionUiEvent.LoadMore(id)) },
        )
    }
}

@Composable
private fun QuestionElement(
    element: DetailElement,
    isExpanded: Boolean,
    onOverflow: (Boolean) -> Unit,
    onImageClick: (ZhihuImage) -> Unit,
    onUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var localLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier = modifier) {
        when (element) {
            is DetailElement.Text -> {
                SelectionContainer {
                    Text(
                        text = element.content.withThemeSpans(),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        onTextLayout = {
                            localLayoutResult = it
                            if (!isExpanded) {
                                onOverflow(it.isOverflowed())
                            }
                        },
                        modifier = Modifier.pointerInput(element.content) {
                            detectTapGestures { offset ->
                                localLayoutResult?.let { result ->
                                    val position = result.getOffsetForPosition(offset)
                                    element.content.getStringAnnotations(
                                        tag = AnnotatedStringBuilder.URL_TAG,
                                        start = position,
                                        end = position,
                                    ).firstOrNull()?.let { annotation ->
                                        onUrlClick(annotation.item)
                                    }
                                }
                            }
                        },
                    )
                }
            }

            is DetailElement.Image -> {
                ImageComponent(element.image, onImageClick)
            }
        }
    }
}

/**
 * Who asked the question, drawn as a column's author is: a small picture, with the name and
 * headline stacked beside it.
 */
@Composable
private fun QuestionAuthor(
    author: QuestionAuthorDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = sizedImageUrl(author.avatarUrl, 32.dp),
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = author.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (author.headline.isNotBlank()) {
                Text(
                    text = author.headline,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@AppPreviews
@Composable
private fun QuestionAuthorPreview() {
    PreviewSurface {
        QuestionAuthor(
            author = QuestionAuthorDto(
                profileToken = "ada-lovelace",
                name = "Ada Lovelace",
                headline = "Writes about parsers and the machines that read them",
                avatarUrl = null,
            ),
            onClick = {},
        )
    }
}

@Composable
private fun TopicRow(
    topics: List<Topic>,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(topics, key = { it.id }) { topic ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clickable { /* TODO: Topic Detail */ },
            ) {
                Text(
                    text = topic.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ExpandToggleButton(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (isExpanded) {
                    stringResource(R.string.action_collapse)
                } else {
                    stringResource(R.string.action_expand)
                },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation),
            )
        }
    }
}

/**
 * The collapsed bar. Its title sits behind a derived state: read straight, the collapse fraction
 * is a new value every frame and the title would recompose for each of them.
 */
@Composable
private fun QuestionTopBar(
    state: QuestionViewModel.QuestionUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LargeTopAppBar(
        modifier = modifier,
        title = {
            val titleText = state.question?.title ?: ""
            val isCollapsed by remember(scrollBehavior) {
                derivedStateOf { scrollBehavior.state.collapsedFraction > 0.5f }
            }
            Text(
                text = titleText,
                modifier = Modifier.padding(end = 12.dp),
                style = if (isCollapsed) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = if (isCollapsed) 1 else 3,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        ),
    )
}

@AppPreviews
@Composable
private fun TopicRowPreview() {
    PreviewSurface {
        TopicRow(topics = sampleTopics)
    }
}

@AppPreviews
@Composable
private fun ExpandToggleButtonPreview() {
    PreviewSurface {
        Column {
            ExpandToggleButton(isExpanded = false, onClick = {})
            ExpandToggleButton(isExpanded = true, onClick = {})
        }
    }
}
