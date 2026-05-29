package com.prslc.zhiflow.ui.page.people

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.LoadingView
import com.prslc.zhiflow.ui.page.people.moment.PeopleActivitiesTab
import com.prslc.zhiflow.ui.page.people.moment.PeoplePostsTab
import com.prslc.zhiflow.ui.page.people.moment.PeopleUpvotesTab
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val TAB_COUNT = 3
private val TOP_BAR_HEIGHT = 48.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PeopleScreen(
    urlToken: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PeopleViewModel = koinViewModel(),
) {
    val uiState = viewModel.uiState
    val pagerState = rememberPagerState(pageCount = { TAB_COUNT })
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val scrollState = remember(viewModel) {
        PeopleTabBarState(
            onOffsetChanged = { viewModel.headerScrollOffset = it },
            getOffset = { viewModel.headerScrollOffset },
        )
    }

    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
    val topBarHeightPx = with(density) { TOP_BAR_HEIGHT.toPx() }
    scrollState.totalTopHeightPx = statusBarHeightPx + topBarHeightPx

    LaunchedEffect(urlToken) {
        viewModel.loadPeople(urlToken)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .nestedScroll(scrollState.nestedScrollConnection)
        ) {
            when {
                uiState.user != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY =
                                    scrollState.compensatedHeaderHeight + viewModel.headerScrollOffset
                                alpha = if (scrollState.compensatedHeaderHeight > 0f) 1f else 0f
                            }
                    ) {
                        PeopleTabBar(
                            pagerState = pagerState,
                            onTabSelected = { index ->
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                            modifier = Modifier.shadow(if (scrollState.isTabsPinned) 2.dp else 0.dp),
                        )

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> PeoplePostsTab(urlToken = urlToken)
                                1 -> PeopleActivitiesTab(urlToken = urlToken)
                                2 -> PeopleUpvotesTab(urlToken = urlToken)
                            }
                        }
                    }

                   // header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { scrollState.headerHeightPx = it.height.toFloat() }
                            .graphicsLayer { translationY = viewModel.headerScrollOffset }
                    ) {
                        PeopleHeader(user = uiState.user)
                    }

                    // topbar
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = scrollState.topBarAlpha),
                        shadowElevation = if (scrollState.topBarAlpha > 0.9f && !scrollState.isTabsPinned) 2.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .height(TOP_BAR_HEIGHT)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.general_back),
                                    tint = if (scrollState.topBarAlpha > 0.5f) MaterialTheme.colorScheme.onSurface else Color.White,
                                )
                            }

                            if (scrollState.topBarAlpha > 0.8f) {
                                Text(
                                    text = uiState.user.name.orEmpty(),
                                    modifier = Modifier.align(Alignment.Center),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            IconButton(
                                onClick = { /* More */ },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.general_more),
                                    tint = if (scrollState.topBarAlpha > 0.5f) MaterialTheme.colorScheme.onSurface else Color.White,
                                )
                            }
                        }
                    }
                }

                uiState.isLoading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { LoadingView() }
                }

                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorView(
                            message = uiState.error.uiMessage,
                            onRetry = { viewModel.loadPeople(urlToken) },
                        )
                    }
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.general_back),
                        )
                    }
                }
            }
        }
    }
}