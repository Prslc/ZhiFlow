package com.prslc.zhiflow.ui.page.people

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.StatusBarIconEffect
import com.prslc.zhiflow.ui.component.common.iconScrim
import com.prslc.zhiflow.ui.component.common.rememberActionErrorHost
import com.prslc.zhiflow.ui.page.people.moment.PeopleActivitiesTab
import com.prslc.zhiflow.ui.page.people.moment.PeoplePostsTab
import com.prslc.zhiflow.ui.page.people.moment.PeopleUpvotesTab
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private const val TAB_COUNT = 3
private val TOP_BAR_HEIGHT = 48.dp

/** M3's top app bars inset their actions by 4dp; flush to the edge reads as clipped. */
private val TOP_BAR_SIDE_PADDING = 4.dp

// The loaded page resolves over the loading one rather than cutting to it: a bright cover landing
// on a black screen in a single frame is what reads as a flash.
private const val CONTENT_FADE_MS = 120

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


    // White status bar icons while the cover image is behind the status bar;
    // dark icons once the opaque top bar scrolls underneath.
    StatusBarIconEffect(darkIcons = scrollState.topBarAlpha > 0.9f)
    LaunchedEffect(urlToken) {
        viewModel.loadPeople(urlToken)
    }

    val snackbarHostState = rememberActionErrorHost(
        viewModel.actionError,
        viewModel::consumeActionError,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .nestedScroll(scrollState.nestedScrollConnection)
        ) {
            AnimatedContent(
                targetState = uiState.user,
                contentKey = { it != null },
                transitionSpec = {
                    fadeIn(tween(CONTENT_FADE_MS)) togetherWith fadeOut(tween(CONTENT_FADE_MS))
                },
                label = "PeopleScreen",
                modifier = Modifier.fillMaxSize(),
            ) { user ->
                // Wrapped in a box because AnimatedContent measures its content with the
                // constraints it was handed: a bar that sizes itself is stretched without one.
                Box {
                    when {
                        user != null -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .collapsingHeader { viewModel.headerScrollOffset }
                                        // Inside the collapsing modifier on purpose: the scroll
                                        // limits want the header's own height, not the part of it
                                        // left on screen.
                                        .onSizeChanged { scrollState.headerHeightPx = it.height.toFloat() },
                                ) {
                                    PeopleHeader(user = user, onFollowClick = viewModel::toggleFollow)
                                }

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
                                        .padding(horizontal = TOP_BAR_SIDE_PADDING)
                                ) {
                                    IconButton(
                                        onClick = onBack,
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .iconScrim(0.55f * (1f - scrollState.topBarAlpha))
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.general_back),
                                            tint = if (scrollState.topBarAlpha > 0.5f) MaterialTheme.colorScheme.onSurface else Color.White,
                                        )
                                    }

                                    if (scrollState.topBarAlpha > 0.8f) {
                                        Text(
                                            text = user.name.orEmpty(),
                                            modifier = Modifier.align(Alignment.Center),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }

                                    IconButton(
                                        onClick = { /* More */ },
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .iconScrim(0.55f * (1f - scrollState.topBarAlpha))
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

                        // Empty, and it has to stay sized: this is also the copy that fades out
                        // under the loaded one, so it cannot collapse. Nothing is drawn while it
                        // loads.
                        else -> Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

/**
 * Hands back only as much of the header as is still on screen: the scroll offset comes off its
 * height as well as its position, so the tabs and the feeds under it rise with it and stay under
 * it. The header is measured whole either way, so nothing is cut off — only the room it claims.
 *
 * It belongs in the column with them rather than over them: a row laid out after it is placed
 * against the height this pass reports, where an overlay leaves it a frame behind, measured off
 * state.
 */
private fun Modifier.collapsingHeader(offset: () -> Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val offsetPx = offset().roundToInt()
        layout(placeable.width, (placeable.height + offsetPx).coerceAtLeast(0)) {
            placeable.placeRelative(0, offsetPx)
        }
    }
