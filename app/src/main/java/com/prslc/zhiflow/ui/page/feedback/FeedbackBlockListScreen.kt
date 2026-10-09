package com.prslc.zhiflow.ui.page.feedback

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.data.dto.KeywordIssue
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.rememberActionErrorHost
import com.prslc.zhiflow.ui.navigation.FeedbackBlockList
import org.koin.androidx.compose.koinViewModel

/**
 * Picks the topics and keywords to filter out. Submitting sends the page's whole state,
 * so what is on screen is exactly what gets blocked.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedbackBlockListScreen(
    route: FeedbackBlockList,
    onBlocked: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedbackBlockListViewModel = koinViewModel(),
) {
    val state = viewModel.uiState
    val loadError = state.loadError
    val isReady = !state.isLoading && loadError == null

    val context = LocalContext.current
    val submittedMessage = stringResource(R.string.feedback_block_submitted)
    val snackbarHostState = rememberActionErrorHost(state.actionError, viewModel::consumeActionError)

    LaunchedEffect(route) { viewModel.load(route) }

    LaunchedEffect(state.isSubmitted) {
        if (state.isSubmitted) {
            Toast.makeText(context, submittedMessage, Toast.LENGTH_SHORT).show()
            onBlocked(route.contentToken)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feedback_block_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.general_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (isReady) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Button(
                        onClick = viewModel::submit,
                        enabled = !state.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(R.string.feedback_block_confirm))
                        }
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingView(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            )

            loadError != null -> ErrorView(
                message = loadError.uiMessage,
                onRetry = viewModel::retry,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            )

            else -> BlockForm(
                state = state,
                onToggleTag = viewModel::toggleTag,
                onInputChange = viewModel::onInputChange,
                onAddKeyword = viewModel::addKeyword,
                onRemoveKeyword = viewModel::removeKeyword,
                enabled = !state.isSubmitting,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockForm(
    state: FeedbackBlockListViewModel.BlockUiState,
    onToggleTag: (Long) -> Unit,
    onInputChange: (String) -> Unit,
    onAddKeyword: () -> Unit,
    onRemoveKeyword: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        if (state.tags.isNotEmpty()) {
            SectionTitle(stringResource(R.string.feedback_block_tags))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.tags.forEach { tag ->
                    FilterChip(
                        selected = tag.isSelected,
                        onClick = { onToggleTag(tag.id) },
                        label = { Text(tag.label) },
                        enabled = enabled,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SectionTitle(stringResource(R.string.feedback_block_keywords))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.keywords.forEach { keyword ->
                InputChip(
                    selected = false,
                    onClick = { onRemoveKeyword(keyword) },
                    label = { Text(keyword) },
                    enabled = enabled,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.feedback_block_remove),
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.Top) {
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                singleLine = true,
                isError = state.keywordIssue != null,
                placeholder = { Text(stringResource(R.string.feedback_block_keyword_hint)) },
                supportingText = { Text(keywordHint(state)) },
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAddKeyword,
                enabled = enabled && state.input.isNotBlank(),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.feedback_block_add))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
@ReadOnlyComposable
private fun keywordHint(state: FeedbackBlockListViewModel.BlockUiState): String =
    when (state.keywordIssue) {
        KeywordIssue.TOO_SHORT ->
            stringResource(R.string.feedback_block_keyword_too_short, state.minLength)

        KeywordIssue.TOO_LONG ->
            stringResource(R.string.feedback_block_keyword_too_long, state.maxLength)

        KeywordIssue.MAX_COUNT ->
            stringResource(R.string.feedback_block_keyword_max_count, state.maxCount)

        KeywordIssue.DUPLICATE -> stringResource(R.string.feedback_block_keyword_duplicate)

        null -> stringResource(
            R.string.feedback_block_keyword_rule,
            state.minLength,
            state.maxLength,
            state.maxCount,
        )
    }
