package com.prslc.zhiflow.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import com.prslc.zhiflow.data.model.AnswerAuthor
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.ui.component.ImageLightbox
import com.prslc.zhiflow.ui.component.RichText
import com.prslc.zhiflow.ui.viewmodel.AnswerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerScreen(
    answerId: String,
    onBack: () -> Unit,
    viewModel: AnswerViewModel = viewModel()
) {
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

    // loading
    if (viewModel.isLoading && currentAnswer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(strokeWidth = 3.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.general_loding),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        return
    }

    // error
    if (viewModel.errorMessage != null && currentAnswer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ErrorMessage(viewModel.errorMessage!!, Modifier)
        }
        return
    }

    // content
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        val titleText = currentAnswer?.question?.title
                            ?: currentAnswer?.header?.text
                            ?: stringResource(R.string.question_title_filed)

                        val isCollapsed = scrollBehavior.state.collapsedFraction > 0.5f

                        Text(
                            text = titleText,
                            modifier = Modifier.padding(
                                end = if (isCollapsed) 10.dp else 5.dp
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
            }
        ) { padding ->
            currentAnswer?.let { answer ->
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 50.dp)
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
        // light box
        ImageLightbox(
            imageUrl = selectedImageUrl,
            onDismiss = { selectedImageUrl = null }
        )
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
fun ErrorMessage(
    message: String,
    modifier: Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, color = MaterialTheme.colorScheme.error)
    }
}