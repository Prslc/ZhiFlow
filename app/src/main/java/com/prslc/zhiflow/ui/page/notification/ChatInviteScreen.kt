package com.prslc.zhiflow.ui.page.notification

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.shouldLoadMore
import com.prslc.zhiflow.ui.component.common.EmptyView
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.PersonRow
import com.prslc.zhiflow.ui.component.common.pagingFooter
import com.prslc.zhiflow.ui.navigation.CHAT_SOURCE_SEED
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import org.koin.androidx.compose.koinViewModel

/**
 * Who a conversation can be started with: a box to name someone in, and the people the server
 * hands back.
 *
 * A conversation opened this way is one that may not exist yet, which is what the server is told
 * the page was opened by.
 *
 * @param onBack Called by the back arrow.
 * @param modifier Applied to the scaffold.
 * @param viewModel The page's own; a caller passes one only to test.
 */
@Composable
fun ChatInviteScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatInviteViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    val uiState = viewModel.uiState
    val error = uiState.error
    val query = viewModel.query
    val isSearch = query.isNotBlank()
    val people = if (isSearch) uiState.found else uiState.followees

    LaunchedEffect(Unit) { viewModel.loadFollowees() }

    // One list replaces the other, and a list opened where the other was left would start at
    // whatever the reader had scrolled to; each source starts at its own top.
    LaunchedEffect(isSearch) { viewModel.listState.scrollToItem(0) }

    val shouldLoadMore by remember { viewModel.listState.shouldLoadMore() }
    LaunchedEffect(shouldLoadMore) {
        // The list the reader follows is the one that pages; a search answers what it was asked.
        if (shouldLoadMore && !isSearch) viewModel.loadMore()
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
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text(stringResource(R.string.chat_invite_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        // Without this the field is only as wide as what is typed into it.
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (people.isEmpty()) {
                when {
                    uiState.isLoading || uiState.isSearching -> LoadingView(modifier = Modifier.fillMaxSize())

                    error != null -> ErrorView(
                        message = error.uiMessage,
                        onRetry = { if (isSearch) viewModel.retrySearch() else viewModel.loadFollowees() },
                        modifier = Modifier.fillMaxSize(),
                    )

                    else -> EmptyView(
                        message = stringResource(
                            if (isSearch) R.string.chat_invite_empty else R.string.chat_invite_nobody_followed,
                        ),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                LazyColumn(
                    state = viewModel.listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(people, key = { it.id }) { person ->
                        PersonRow(
                            person = person,
                            onClick = { navigator.navigateToChat(person.id, CHAT_SOURCE_SEED) },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )
                    }

                    if (!isSearch) {
                        pagingFooter(
                            isLoading = uiState.isNextLoading,
                            error = uiState.loadMoreError,
                            onRetry = viewModel::loadMore,
                        )
                    }
                }
            }
        }
    }
}
