package com.prslc.zhiflow.ui.page.people

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.utils.compose.shouldLoadMore
import com.prslc.zhiflow.data.model.user.PeopleListKind
import com.prslc.zhiflow.ui.component.common.EmptyView
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.component.common.PersonRow
import com.prslc.zhiflow.ui.component.common.pagingFooter
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import org.koin.androidx.compose.koinViewModel

/**
 * The people an account follows, or the people following it — one list, one page.
 *
 * @param userId Whose list this is: the account's hash id.
 * @param kind Which of the two lists to read.
 * @param onBack Called by the back arrow.
 * @param modifier Applied to the scaffold.
 * @param viewModel The page's own; a caller passes one only to test.
 */
@Composable
fun PeopleListScreen(
    userId: String,
    kind: PeopleListKind,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PeopleListViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
    LaunchedEffect(userId, kind) { viewModel.loadIfEmpty(userId, kind) }

    val shouldLoadMore by remember { viewModel.listState.shouldLoadMore() }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    val uiState = viewModel.uiState
    val people = uiState.people
    val endText = if (uiState.isEnd && people.isNotEmpty()) stringResource(R.string.end_of_list) else null
    val titleRes = when (kind) {
        PeopleListKind.FOLLOWING -> R.string.people_list_following
        PeopleListKind.FOLLOWERS -> R.string.people_list_followers
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.general_back),
                        )
                    }
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
            when {
                people.isEmpty() && uiState.isRefreshing -> {
                    LoadingView(modifier = Modifier.fillMaxSize())
                }

                people.isEmpty() && uiState.globalError != null -> {
                    ErrorView(
                        message = uiState.globalError.uiMessage,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                people.isEmpty() -> {
                    EmptyView(
                        message = stringResource(R.string.people_list_empty),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    LazyColumn(
                        state = viewModel.listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items(people, key = { it.id }) { person ->
                            PersonRow(
                                person = person,
                                onClick = { navigator.navigateToPeople(person.profileToken) },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                        }

                        pagingFooter(
                            isLoading = uiState.isNextLoading,
                            error = uiState.loadMoreError,
                            onRetry = viewModel::loadMore,
                            endText = endText,
                        )
                    }
                }
            }
        }
    }
}
