package com.prslc.zhiflow.ui.page.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.shouldLoadMore
import com.prslc.zhiflow.data.dto.SearchItemDto
import com.prslc.zhiflow.ui.component.common.EmptyView
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.pagingFooter
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    val focusManager = LocalFocusManager.current
    val listState = viewModel.listState

    LaunchedEffect(Unit) { viewModel.loadHotQueries() }

    val shouldLoadMore by remember { listState.shouldLoadMore() }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    val uiState = viewModel.uiState
    val endText = if (uiState.isEnd) stringResource(R.string.end_of_list) else null
    val search = remember(viewModel, focusManager) {
        {
            viewModel.submit(viewModel.query)
            focusManager.clearFocus()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.general_back),
                        )
                    }
                },
                title = {
                    TextField(
                        value = viewModel.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        singleLine = true,
                        // Without this the field is only as wide as what is typed into it, and
                        // the clear button rides along with the last character.
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (viewModel.query.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.onQueryChange("")
                                        viewModel.clearResults()
                                    },
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        stringResource(R.string.search_action_clear),
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                viewModel.submittedQuery.isNotEmpty() -> Results(
                    uiState = uiState,
                    listState = listState,
                    endText = endText,
                    onItemClick = navigator::navigateToContent,
                    onPhraseClick = { viewModel.submit(it) },
                    onRetry = { viewModel.submit(viewModel.submittedQuery) },
                    onLoadMore = viewModel::loadMore,
                )

                viewModel.suggestions.isNotEmpty() -> PhraseList(
                    phrases = viewModel.suggestions,
                    hotPhrases = emptySet(),
                    onClick = { viewModel.submit(it) },
                )

                else -> PhraseList(
                    phrases = viewModel.hotQueries.map { it.query },
                    hotPhrases = viewModel.hotQueries.filter { it.isHot }
                        .map { it.query }
                        .toSet(),
                    onClick = { viewModel.submit(it) },
                )
            }
        }
    }
}

@Composable
private fun Results(
    uiState: SearchViewModel.SearchUiState,
    listState: LazyListState,
    endText: String?,
    onItemClick: (String, String) -> Unit,
    onPhraseClick: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    when {
        uiState.isSearching -> LoadingView(modifier = Modifier.fillMaxSize())

        uiState.globalError != null -> ErrorView(
            message = uiState.globalError.uiMessage,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize(),
        )

        uiState.items.isEmpty() -> EmptyView(
            message = stringResource(R.string.search_empty),
            modifier = Modifier.fillMaxSize(),
        )

        else -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(uiState.items) { item ->
                when (item) {
                    is SearchItemDto.Article -> SearchArticleCard(item, onItemClick)
                    is SearchItemDto.RelatedQueries -> RelatedQueriesRow(item, onPhraseClick)
                }
                HorizontalDivider()
            }

            pagingFooter(
                isLoading = uiState.isNextLoading,
                error = uiState.loadMoreError,
                onRetry = onLoadMore,
                endText = endText,
            )
        }
    }
}

/** The phrases to search for next: the suggestions while typing, the hot ones before that. */
@Composable
private fun PhraseList(
    phrases: List<String>,
    hotPhrases: Set<String>,
    onClick: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(phrases) { phrase ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(phrase) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (phrase in hotPhrases) {
                        Icons.Filled.LocalFireDepartment
                    } else {
                        Icons.Filled.Search
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = phrase,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
