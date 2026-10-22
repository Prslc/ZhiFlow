package com.prslc.zhiflow.ui.page.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.shouldLoadMore
import com.prslc.zhiflow.data.dto.SearchItemDto
import com.prslc.zhiflow.data.model.search.SearchTab
import com.prslc.zhiflow.ui.component.common.EmptyView
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.pagingFooter
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface
import com.prslc.zhiflow.ui.preview.sampleHotQueries
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchScreen(
    tab: SearchTab,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = viewModel.listState
    val historyEnabled by viewModel.historyEnabled.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    // The hot searches are only worth asking for when they are what the box is going to draw.
    LaunchedEffect(tab, historyEnabled) {
        if (!historyEnabled) viewModel.loadHotQueries(tab)
    }

    val shouldLoadMore by remember { listState.shouldLoadMore() }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    val uiState = viewModel.uiState
    val endText = if (uiState.isEnd) stringResource(R.string.end_of_list) else null

    // A search replaces the box's text while it is still focused: end the editing session, or the
    // keyboard stays over the results and the cursor stays where it was in the old text.
    LaunchedEffect(uiState.isSearching) {
        if (uiState.isSearching) {
            focusManager.clearFocus()
            keyboardController?.hide()
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
                        placeholder = { Text(stringResource(tab.placeholderRes)) },
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
                        keyboardActions = KeyboardActions(
                            onSearch = { viewModel.submit(viewModel.query, tab) },
                        ),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (viewModel.refinements.isNotEmpty()) {
                RefinementRow(
                    refinements = viewModel.refinements,
                    onClick = { viewModel.submit(it, tab) },
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    viewModel.submittedQuery.isNotEmpty() -> Results(
                        uiState = uiState,
                        listState = listState,
                        endText = endText,
                        onItemClick = navigator::navigateToContent,
                        onPhraseClick = { viewModel.submit(it, tab) },
                        onRetry = { viewModel.submit(viewModel.submittedQuery, tab) },
                        onLoadMore = viewModel::loadMore,
                    )

                    viewModel.suggestions.isNotEmpty() -> PhraseList(
                        phrases = viewModel.suggestions,
                        hotPhrases = emptySet(),
                        onClick = { viewModel.submit(it, tab) },
                    )

                    historyEnabled -> if (history.isEmpty()) {
                        EmptyView(
                            message = stringResource(R.string.search_history_empty),
                            modifier = Modifier.fillMaxSize(),
                            icon = Icons.Filled.History,
                        )
                    } else {
                        HistoryList(
                            phrases = history,
                            onPhraseClick = { viewModel.submit(it, tab) },
                            onPhraseRemove = viewModel::removeHistory,
                            onClear = viewModel::clearHistory,
                        )
                    }

                    else -> PhraseList(
                        phrases = viewModel.hotQueries.map { it.query },
                        hotPhrases = viewModel.hotQueries.filter { it.isHot }
                            .map { it.query }
                            .toSet(),
                        onClick = { viewModel.submit(it, tab) },
                    )
                }
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
            items(
                items = uiState.items,
                key = { it.searchKey },
                contentType = { it.searchContentType },
            ) { item ->
                when (item) {
                    is SearchItemDto.Content -> SearchResultCard(item, onItemClick)
                    is SearchItemDto.RelatedQueries -> RelatedQueriesRow(item, onPhraseClick)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
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

/** The box's placeholder: one names the whole site, the other what the page is scoped to. */
private val SearchTab.placeholderRes: Int
    get() = when (this) {
        SearchTab.GENERAL -> R.string.search_placeholder
        SearchTab.FAVLIST -> R.string.search_placeholder_collections
    }

/** The phrases to search for next: the suggestions while typing, the hot ones before that. */
@Composable
private fun PhraseList(
    phrases: List<String>,
    hotPhrases: Set<String>,
    onClick: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(phrases, key = { it }) { phrase ->
            PhraseRow(
                icon = if (phrase in hotPhrases) {
                    Icons.Filled.LocalFireDepartment
                } else {
                    Icons.Filled.Search
                },
                phrase = phrase,
                onClick = onClick,
            )
        }
    }
}

/** What the reader has searched for before, each with the way to put it away. */
@Composable
private fun HistoryList(
    phrases: List<String>,
    onPhraseClick: (String) -> Unit,
    onPhraseRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(phrases, key = { it }) { phrase ->
            PhraseRow(
                icon = Icons.Filled.History,
                phrase = phrase,
                onClick = onPhraseClick,
                trailing = {
                    IconButton(onClick = { onPhraseRemove(phrase) }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(
                                R.string.search_history_remove,
                                phrase,
                            ),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
        item {
            Text(
                text = stringResource(R.string.search_history_clear),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClear)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            )
        }
    }
}

/**
 * One phrase on offer, whichever list it came from.
 *
 * [trailing] is drawn over the row's end rather than beside it, so a button there does not make
 * the row taller than the line of text it belongs to.
 */
@Composable
private fun PhraseRow(
    icon: ImageVector,
    phrase: String,
    onClick: (String) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(phrase) }
                .padding(
                    start = 20.dp,
                    top = 14.dp,
                    end = if (trailing == null) 20.dp else 56.dp,
                    bottom = 14.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
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
        if (trailing != null) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd),
                contentAlignment = Alignment.Center,
            ) {
                trailing()
            }
        }
    }
}

/** The list before anything is typed: the hot phrases, marked, and the rest beside them. */
@AppPreviews
@Composable
private fun PhraseListPreview() {
    PreviewSurface {
        PhraseList(
            phrases = sampleHotQueries.map { it.query },
            hotPhrases = sampleHotQueries.filter { it.isHot }.map { it.query }.toSet(),
            onClick = {},
        )
    }
}

/** The list before anything is typed when the reader keeps a history instead. */
@AppPreviews
@Composable
private fun HistoryListPreview() {
    PreviewSurface {
        HistoryList(
            phrases = listOf("Jetpack Compose", "Kotlin coroutines", "Machine learning"),
            onPhraseClick = {},
            onPhraseRemove = {},
            onClear = {},
        )
    }
}
