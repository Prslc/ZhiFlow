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
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import com.prslc.zhiflow.data.model.user.PeopleListKind
import com.prslc.zhiflow.ui.component.common.ErrorView
import com.prslc.zhiflow.ui.component.common.StatusBarIconEffect
import com.prslc.zhiflow.ui.component.common.iconScrim
import com.prslc.zhiflow.ui.component.common.rememberActionErrorHost
import com.prslc.zhiflow.ui.navigation.CHAT_SOURCE_SEED
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.page.people.moment.PeopleActivitiesTab
import com.prslc.zhiflow.ui.page.people.moment.PeoplePostsTab
import com.prslc.zhiflow.ui.page.people.moment.PeopleUpvotesTab
import com.prslc.zhiflow.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private const val TAB_COUNT = 3
private val TOP_BAR_HEIGHT = 48.dp

/** M3's top app bars inset their actions by 4dp; flush to the edge reads as clipped. */
private val TOP_BAR_SIDE_PADDING = 4.dp

// The loaded page resolves over the loading one rather than cutting to it: a bright cover landing
// on a black screen in a single frame is what reads as a flash.
private const val CONTENT_FADE_MS = 120

// How long to let a released drag start settling before stepping in: a finger going up and a
// fling starting are a frame or two apart, and between them neither one holds the scroll —
// which is the shape of the state this waits out.
private const val SETTLE_GRACE_MS = 150L

@Composable
fun PeopleScreen(
    urlToken: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PeopleViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current
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

    // White status bar icons while the cover image is behind the status bar; once the opaque top
    // bar is underneath they follow the theme, that bar being the theme's own surface.
    StatusBarIconEffect(
        darkIcons = scrollState.isTopBarOpaque && !LocalIsDarkTheme.current,
    )
    LaunchedEffect(urlToken) {
        viewModel.loadPeople(urlToken)
    }

    // A flick can leave the pager between two pages: the release snap targets a point a few pixels
    // away and finishes having moved almost nothing. Hand it the settle it never ran.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress to pagerState.currentPageOffsetFraction }
            .collect { (isScrolling, offsetFraction) ->
                if (isScrolling || offsetFraction == 0f) return@collect
                delay(SETTLE_GRACE_MS.milliseconds)
                if (pagerState.isParked()) pagerState.animateScrollToPage(pagerState.currentPage)
            }
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
                .nestedScroll(scrollState.nestedScrollConnection),
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
                                        // Inside the collapsing modifier: the scroll limits want the
                                        // header's own height, not what is left on screen.
                                        .onSizeChanged { scrollState.headerHeightPx = it.height.toFloat() },
                                ) {
                                    PeopleHeader(
                                        user = user,
                                        onFollowClick = viewModel::toggleFollow,
                                        onMessageClick = {
                                            navigator.navigateToChat(user.id, CHAT_SOURCE_SEED)
                                        },
                                        onFollowersClick = {
                                            navigator.navigateToPeopleList(
                                                user.id,
                                                PeopleListKind.FOLLOWERS,
                                            )
                                        },
                                        onFollowingClick = {
                                            navigator.navigateToPeopleList(
                                                user.id,
                                                PeopleListKind.FOLLOWING,
                                            )
                                        },
                                    )
                                }

                                PeopleTabBar(
                                    pagerState = pagerState,
                                    onTabSelected = { index ->
                                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                    },
                                    modifier = Modifier.shadow(if (scrollState.tabsPinned) 2.dp else 0.dp),
                                )

                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                ) { page ->
                                    when (page) {
                                        0 -> PeoplePostsTab(urlToken = urlToken)
                                        1 -> PeopleActivitiesTab(urlToken = urlToken)
                                        2 -> PeopleUpvotesTab(urlToken = urlToken)
                                    }
                                }
                            }

                            // topbar
                            // Drawn, not composed: a color fade would be a new value every frame.
                            val topBarSurface = MaterialTheme.colorScheme.surface
                            Surface(
                                color = Color.Transparent,
                                shadowElevation = if (scrollState.isTopBarOpaque && !scrollState.tabsPinned) 2.dp else 0.dp,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .drawBehind {
                                            drawRect(
                                                color = topBarSurface,
                                                alpha = scrollState.topBarAlpha,
                                            )
                                        }
                                        .statusBarsPadding()
                                        .height(TOP_BAR_HEIGHT)
                                        .padding(horizontal = TOP_BAR_SIDE_PADDING),
                                ) {
                                    IconButton(
                                        onClick = onBack,
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .iconScrim { 0.55f * (1f - scrollState.topBarAlpha) },
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.general_back),
                                            tint = if (scrollState.iconsOnSurface) MaterialTheme.colorScheme.onSurface else Color.White,
                                        )
                                    }

                                    if (scrollState.isTitleShown) {
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
                                            .iconScrim { 0.55f * (1f - scrollState.topBarAlpha) },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.general_more),
                                            tint = if (scrollState.iconsOnSurface) MaterialTheme.colorScheme.onSurface else Color.White,
                                        )
                                    }
                                }
                            }
                        }

                        uiState.globalError != null -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                ErrorView(
                                    message = uiState.globalError.uiMessage,
                                    onRetry = { viewModel.loadPeople(urlToken) },
                                )
                            }
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.general_back),
                                )
                            }
                        }

                        // Empty but sized: this is the copy that fades out under the loaded one,
                        // so it cannot collapse while it loads.
                        else -> {
                            Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

/**
 * Whether the pager is sitting between two pages with nothing on its way to move it.
 *
 * A flick is the way in: the snap that follows it comes to rest a few pixels from where the
 * pager already was, which leaves the pager parked and takes no notice of the page boundary.
 */
private fun PagerState.isParked(): Boolean =
    !isScrollInProgress && currentPageOffsetFraction != 0f

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
