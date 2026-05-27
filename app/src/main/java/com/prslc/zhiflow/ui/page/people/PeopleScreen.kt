package com.prslc.zhiflow.ui.page.people

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
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
import com.prslc.zhiflow.ui.page.people.moment.AutoLoadMoreEffect
import com.prslc.zhiflow.ui.page.people.moment.MomentViewModel
import com.prslc.zhiflow.ui.page.people.moment.momentsContent
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
    momentViewModel: MomentViewModel = koinViewModel()
) {
    val uiState = viewModel.uiState
    val pagerState = rememberPagerState(pageCount = { TAB_COUNT })
    val coroutineScope = rememberCoroutineScope()
    val listState = momentViewModel.listState
    val density = LocalDensity.current

    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
    val topBarHeightPx = with(density) { TOP_BAR_HEIGHT.toPx() }
    val totalTopHeightPx = statusBarHeightPx + topBarHeightPx

    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val maxUpwardScrollPx = remember(headerHeightPx, totalTopHeightPx) {
        (headerHeightPx - totalTopHeightPx).coerceAtLeast(0f)
    }

    var headerScrollOffset by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember(maxUpwardScrollPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0 && maxUpwardScrollPx > 0f) {
                    val previousOffset = headerScrollOffset
                    headerScrollOffset =
                        (headerScrollOffset + delta).coerceIn(-maxUpwardScrollPx, 0f)
                    return Offset(0f, headerScrollOffset - previousOffset)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0 && maxUpwardScrollPx > 0f) {
                    val previousOffset = headerScrollOffset
                    headerScrollOffset =
                        (headerScrollOffset + delta).coerceIn(-maxUpwardScrollPx, 0f)
                    return Offset(0f, headerScrollOffset - previousOffset)
                }
                return Offset.Zero
            }
        }
    }

    val topBarAlpha by remember(maxUpwardScrollPx) {
        derivedStateOf {
            if (maxUpwardScrollPx > 0f) (-headerScrollOffset / maxUpwardScrollPx).coerceIn(0f, 1f) else 0f
        }
    }

    val isTabsPinned by remember(maxUpwardScrollPx) {
        derivedStateOf { maxUpwardScrollPx > 0f && headerScrollOffset <= -maxUpwardScrollPx }
    }

    LaunchedEffect(urlToken) {
        viewModel.loadPeople(urlToken)
        momentViewModel.loadMoment(urlToken)
    }

    AutoLoadMoreEffect(momentViewModel, pagerState.currentPage)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .nestedScroll(nestedScrollConnection)
        ) {
            val screenHeight = maxHeight
            val pagerContainerHeight = screenHeight - with(density) { totalTopHeightPx.toDp() }

            when {
                uiState.user != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pagerContainerHeight)
                            .graphicsLayer {
                                translationY = headerHeightPx + headerScrollOffset
                                alpha = if (headerHeightPx > 0f) 1f else 0f
                            }
                    ) {
                        PeopleTabBar(
                            pagerState = pagerState,
                            onTabSelected = { index ->
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                            modifier = Modifier.shadow(if (isTabsPinned) 2.dp else 0.dp)
                        )

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        momentsContent(
                                            urlToken = urlToken,
                                            state = momentViewModel.uiState,
                                            viewModel = momentViewModel
                                        )
                                    }
                                }

                                1, 2 -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.general_undeveloped),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { headerHeightPx = it.height.toFloat() }
                            .graphicsLayer { translationY = headerScrollOffset }
                    ) {
                        PeopleHeader(user = uiState.user)
                    }

                    // topbar
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = topBarAlpha),
                        shadowElevation = if (topBarAlpha > 0.9f && !isTabsPinned) 2.dp else 0.dp
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
                                    tint = if (topBarAlpha > 0.5f) MaterialTheme.colorScheme.onSurface else Color.White
                                )
                            }

                            if (topBarAlpha > 0.8f) {
                                Text(
                                    text = uiState.user.name.orEmpty(),
                                    modifier = Modifier.align(Alignment.Center),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { /* More */ },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.general_more),
                                    tint = if (topBarAlpha > 0.5f) MaterialTheme.colorScheme.onSurface else Color.White
                                )
                            }
                        }
                    }
                }

                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingView()
                    }
                }

                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorView(
                            message = uiState.error.uiMessage,
                            onRetry = { viewModel.loadPeople(urlToken) }
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
                            contentDescription = stringResource(R.string.general_back)
                        )
                    }
                }
            }
        }
    }
}
