package com.prslc.zhiflow.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.QuestionDetail
import com.prslc.zhiflow.data.model.ZhihuImage
import com.prslc.zhiflow.parser.model.DetailElement
import com.prslc.zhiflow.ui.component.AnswerDivider
import com.prslc.zhiflow.ui.component.AnswerItem
import com.prslc.zhiflow.ui.component.ImageLightbox
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.viewmodel.QuestionUiState
import com.prslc.zhiflow.ui.viewmodel.QuestionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionDetailScreen(
    id: String,
    onBack: () -> Unit,
    viewModel: QuestionViewModel = viewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var isLightboxVisible by rememberSaveable { mutableStateOf(false) }
    var currentImageIndex by rememberSaveable { mutableIntStateOf(0) }
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val imageUrls by remember(viewModel.uiState) {
        derivedStateOf {
            (viewModel.uiState as? QuestionUiState.Success)?.elements
                ?.filterIsInstance<DetailElement.Image>()
                ?.flatMap { it.image.urls } ?: emptyList()
        }
    }

    LaunchedEffect(id) { viewModel.loadQuestion(id) }

    BackHandler(enabled = isLightboxVisible) { isLightboxVisible = false }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            QuestionTopBar(
                state = viewModel.uiState,
                scrollBehavior = scrollBehavior,
                onBack = onBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val state = viewModel.uiState) {
                is QuestionUiState.Loading -> LoadingView(Modifier.fillMaxSize())
                is QuestionUiState.Error -> ErrorView(
                    message = state.exception.message ?: "Unknown Error",
                    onRetry = { viewModel.loadQuestion(id) },
                    modifier = Modifier.fillMaxSize()
                )

                is QuestionUiState.Success -> {
                    QuestionContentList(
                        state = state,
                        viewModel = viewModel,
                        id = id,
                        isExpanded = isExpanded,
                        onExpandChange = { isExpanded = it },
                        onImageClick = { url ->
                            currentImageIndex = imageUrls.indexOf(url).coerceAtLeast(0)
                            isLightboxVisible = true
                        }
                    )
                }
            }

            if (isLightboxVisible) {
                ImageLightbox(
                    imageUrls = imageUrls,
                    initialIndex = currentImageIndex,
                    onDismiss = { isLightboxVisible = false }
                )
            }
        }
    }
}

@Composable
private fun QuestionContentList(
    state: QuestionUiState.Success,
    viewModel: QuestionViewModel,
    id: String,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onImageClick: (String) -> Unit
) {
    val navigator = LocalNavigator.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // question content
        itemsIndexed(
            items = state.elements,
            key = { index, _ -> "element_$index" }
        ) { index, element ->
            if (isExpanded || index == 0) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    when (element) {
                        is DetailElement.Text -> ExpandableText(
                            content = element.content,
                            isExpanded = isExpanded,
                            showToggle = index == 0,
                            onExpandChange = onExpandChange
                        )

                        is DetailElement.Image -> if (isExpanded) {
                            ImageItem(element.image, onImageClick)
                        }
                    }
                }
            }
        }

        // topic tab
        if (state.question.topics.isNotEmpty()) {
            item(key = "topics_row") {
                TopicRow(topics = state.question.topics)
            }
        }

        // stats
        item(key = "stats_section") {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                QuestionStatsSection(state.question)
                HorizontalDivider(
                    thickness = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        // question list
        itemsIndexed(
            items = viewModel.answerItems,
            key = { _, item -> item.target.id }
        ) { index, feedItem ->
            if (feedItem.targetType == "answer") {
                AnswerItem(
                    target = feedItem.target,
                    onClick = { id ->
                        navigator.navigateToContent(id, "answer")
                    }
                )
                AnswerDivider()
            }
            if (index >= viewModel.answerItems.size - 2) {
                LaunchedEffect(viewModel.answerItems.size) {
                    viewModel.loadMore(id)
                }
            }
        }

        // loading
        if (viewModel.isNextLoading) {
            item(key = "loading_indicator") {
                LoadingFooter()
            }
        }
    }
}

@Composable
private fun TopicRow(topics: List<com.prslc.zhiflow.data.model.Topic>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(topics, key = { it.id }) { topic ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clickable { /* TODO: Topic Detail */ }
            ) {
                Text(
                    text = topic.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ExpandableText(
    content: AnnotatedString,
    isExpanded: Boolean,
    showToggle: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    var canExpand by remember(content) { mutableStateOf(false) }

    Column {
        SelectionContainer {
            Text(
                text = content,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                onTextLayout = { result: TextLayoutResult ->
                    if (!isExpanded) {
                        canExpand = result.didOverflowHeight || result.lineCount > 3
                    }
                }
            )
        }
        if (showToggle && canExpand) {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable { onExpandChange(!isExpanded) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) stringResource(R.string.action_collapse) else stringResource(
                        R.string.action_expand
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionTopBar(
    state: QuestionUiState,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit
) {
    LargeTopAppBar(
        title = {
            val titleText = (state as? QuestionUiState.Success)?.question?.title ?: ""
            val isCollapsed = scrollBehavior.state.collapsedFraction > 0.5f
            Text(
                text = titleText,
                modifier = Modifier.padding(end = 12.dp),
                style = if (isCollapsed) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = if (isCollapsed) 1 else 3,
                overflow = TextOverflow.Ellipsis
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
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    )
}

@Composable
fun QuestionStatsSection(question: QuestionDetail) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.question_stats_answers, question.answerCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = " · ",
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = stringResource(R.string.question_stats_followers, question.followerCount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun ImageItem(image: ZhihuImage, onImageClick: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = image.urls.firstOrNull(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { image.urls.firstOrNull()?.let { onImageClick(it) } },
            contentScale = ContentScale.FillWidth
        )
        image.description.takeIf { it.isNotEmpty() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}