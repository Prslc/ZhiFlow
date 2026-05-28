package com.prslc.zhiflow.ui.page.people.moment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import org.koin.androidx.compose.koinViewModel

@Composable
fun PeopleTabContent(
    urlToken: String,
    viewModel: MomentViewModel,
    modifier: Modifier = Modifier
) {
    val state = viewModel.uiState

    LaunchedEffect(urlToken) {
        if (state.moments.isEmpty() && !state.isLoading && state.error == null) {
            viewModel.loadMoment(urlToken)
        }
    }

    val shouldLoadMore by remember { viewModel.listState.shouldLoadMore() }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.moments.isNotEmpty() -> {
                LazyColumn(
                    state = viewModel.listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    momentsContent(
                        urlToken = urlToken,
                        state = state,
                        viewModel = viewModel
                    )
                }
            }

            state.isLoading -> {
                LoadingView(modifier = Modifier.align(Alignment.Center))
            }

            state.error != null -> {
                ErrorView(
                    message = state.error.uiMessage,
                    onRetry = { viewModel.loadMoment(urlToken) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun PeoplePostsTab(urlToken: String, vm: PostsViewModel = koinViewModel()) =
    PeopleTabContent(urlToken, vm)

@Composable
fun PeopleActivitiesTab(urlToken: String, vm: ActivitiesViewModel = koinViewModel()) =
    PeopleTabContent(urlToken, vm)

@Composable
fun PeopleUpvotesTab(urlToken: String, vm: UpvotesViewModel = koinViewModel()) =
    PeopleTabContent(urlToken, vm)
