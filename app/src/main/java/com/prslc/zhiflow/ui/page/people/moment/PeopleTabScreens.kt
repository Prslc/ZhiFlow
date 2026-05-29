package com.prslc.zhiflow.ui.page.people.moment

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.prslc.zhiflow.core.utils.shouldLoadMore
import org.koin.androidx.compose.koinViewModel

@Composable
fun PeopleTabContent(
    urlToken: String,
    viewModel: MomentViewModel<*>,
    modifier: Modifier = Modifier
) {
    val state = viewModel.uiState

    LaunchedEffect(urlToken) {
        viewModel.loadIfNeeded(urlToken)
    }

    val shouldLoadMore by remember { viewModel.listState.shouldLoadMore() }
    LaunchedEffect(shouldLoadMore, state.isLoading) {
        if (shouldLoadMore && !state.isLoading) {
            viewModel.loadMore()
        }
    }

    LazyColumn(
        state = viewModel.listState,
        modifier = modifier.fillMaxSize()
    ) {
        momentsContent(
            urlToken = urlToken,
            state = state,
            viewModel = viewModel,
        )
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
