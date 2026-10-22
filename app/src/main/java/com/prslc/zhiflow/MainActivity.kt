package com.prslc.zhiflow

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.preference.SettingsStore
import com.prslc.zhiflow.core.preference.ThemeMode
import com.prslc.zhiflow.core.utils.compose.BarCollapseDistance
import com.prslc.zhiflow.core.utils.compose.rememberHideOnScroll
import com.prslc.zhiflow.core.utils.formatCount
import com.prslc.zhiflow.data.dto.UnreadSummaryDto
import com.prslc.zhiflow.data.session.SessionManager
import com.prslc.zhiflow.ui.component.LocalDataSaver
import com.prslc.zhiflow.ui.component.widget.ImageLightboxHost
import com.prslc.zhiflow.ui.navigation.HomeTab
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.navigation.MainContainer
import com.prslc.zhiflow.ui.navigation.MessageTab
import com.prslc.zhiflow.ui.navigation.Navigator
import com.prslc.zhiflow.ui.navigation.ProfileTab
import com.prslc.zhiflow.ui.navigation.contentGraph
import com.prslc.zhiflow.ui.page.feed.FeedScreen
import com.prslc.zhiflow.ui.page.feed.FeedViewModel
import com.prslc.zhiflow.ui.page.feed.NegativeFeedbackSheet
import com.prslc.zhiflow.ui.page.feed.feedbackToastText
import com.prslc.zhiflow.ui.page.notification.MessageScreen
import com.prslc.zhiflow.ui.page.notification.MessageViewModel
import com.prslc.zhiflow.ui.page.profile.ProfileScreen
import com.prslc.zhiflow.ui.theme.ZhiFlowTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {

    private val sessionManager: SessionManager by inject()
    private val settingsStore: SettingsStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by settingsStore.themeMode.collectAsStateWithLifecycle()
            val dataSaver by settingsStore.dataSaver.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            ZhiFlowTheme(darkTheme = darkTheme) {
                Surface(
                    // What puts every testTag below into the accessibility tree as a resource id,
                    // which is the only form UiAutomator -- and so the baseline profile generator --
                    // can match on.
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val context = LocalContext.current
                    val uriHandler = LocalUriHandler.current
                    // One stack for the whole app: the bottom bar lives inside MainContainer, so
                    // a detail covers the tabs rather than belonging to one of them.
                    val backStack = rememberNavBackStack(MainContainer)
                    val navigator = remember(backStack) {
                        Navigator(
                            backStack = backStack,
                            context = context,
                            uriHandler = uriHandler,
                        )
                    }
                    // Held here rather than in MainScreen: the block-list page drops a card the
                    // feed is drawing, and a destination cannot reach the ViewModel of the entry
                    // below it.
                    val feedViewModel: FeedViewModel = koinViewModel()

                    // Sits outside the display, so it stays composed whichever screen is on top.
                    val droppedToGuest = sessionManager.droppedToGuest
                    LaunchedEffect(Unit) {
                        droppedToGuest.collect {
                            Toast.makeText(
                                context,
                                R.string.login_session_expired,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }

                    val imagesSetting = LocalDataSaver provides dataSaver
                    CompositionLocalProvider(LocalNavigator provides navigator, imagesSetting) {
                        NavDisplay(
                            backStack = backStack,
                            onBack = navigator::goBack,
                            // The display adds the saveable-state decorator on its own; the
                            // ViewModel one has to be named, or every destination would share
                            // the activity's store.
                            entryDecorators = listOf(
                                rememberSaveableStateHolderNavEntryDecorator(),
                                rememberViewModelStoreNavEntryDecorator(),
                            ),
                            transitionSpec = {
                                val enter = slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                )
                                val exit = slideOutHorizontally(
                                    targetOffsetX = { -it / 5 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                ) + fadeOut(
                                    targetAlpha = 0f,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                )
                                enter togetherWith exit
                            },
                            popTransitionSpec = {
                                val enter = slideInHorizontally(
                                    initialOffsetX = { -it / 5 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                ) + fadeIn(
                                    initialAlpha = 0f,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                )
                                val exit = slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                )
                                enter togetherWith exit
                            },
                            // Runs during the back gesture, where the pop pair above does not --
                            // the two have to match or the handoff jumps on release.
                            predictivePopTransitionSpec = {
                                val enter = slideInHorizontally(
                                    initialOffsetX = { -it / 5 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                ) + fadeIn(
                                    initialAlpha = 0f,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                )
                                val exit = slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                )
                                enter togetherWith exit
                            },
                            entryProvider = contentGraph(
                                navigator = navigator,
                                feedViewModel = feedViewModel,
                            ),
                        )
                        ImageLightboxHost()
                    }
                }
            }
        }
    }
}

/** The bottom bar's height, and so how far it travels when it goes. */
private val BottomBarHeight = 80.dp

private val TabTags = mapOf<Any, String>(
    HomeTab to "tab_home",
    MessageTab to "tab_message",
    ProfileTab to "tab_profile",
)

/**
 * What the message tab is badged with.
 *
 * It draws the message count, which is the one the page's own mark-all-read clears: a badge drawn
 * from a count no action on that page can move would be a badge that never goes away.
 */
@Composable
private fun UnreadBadge(unread: UnreadSummaryDto?) {
    val messages = unread?.message ?: return
    if (messages.count <= 0) return

    if (messages.showCount) {
        Badge { Text(formatCount(messages.count)) }
    } else {
        Badge()
    }
}

@Composable
fun MainScreen(feedViewModel: FeedViewModel, modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val messageViewModel: MessageViewModel = koinViewModel()

    // The tab's badge is drawn whether or not the page has ever been shown, so the count is fetched
    // from here rather than from the page.
    LaunchedEffect(Unit) { messageViewModel.loadIfEmpty() }

    val tabs = listOf(HomeTab, MessageTab, ProfileTab)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    val bottomBar = rememberHideOnScroll(BarCollapseDistance)
    val bottomBarHeightPx = with(LocalDensity.current) { BottomBarHeight.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .nestedScroll(bottomBar.connection),
        ) { pageIndex ->
            when (tabs[pageIndex]) {
                HomeTab -> FeedScreen(onItemLongClick = feedViewModel::openFeedback)

                ProfileTab -> ProfileScreen(
                    onNavigateToHistory = navigator::navigateToReadHistory,
                    onNavigateToCollections = navigator::navigateToCollectionContents,
                    onNavigateToSettings = navigator::navigateToSettings,
                )

                MessageTab -> MessageScreen(viewModel = messageViewModel)
            }
        }

        // Nothing here changes the layout: a bar at the bottom of the screen has no content under
        // it to push around, so it only has to leave.
        NavigationBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { translationY = bottomBarHeightPx * bottomBar.fraction },
        ) {
            tabs.forEachIndexed { index, tab ->
                NavigationBarItem(
                    modifier = Modifier.testTag(TabTags.getValue(tab)),
                    selected = pagerState.currentPage == index,
                    onClick = {
                        when {
                            pagerState.currentPage != index -> {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }

                            tab == HomeTab -> {
                                scope.launch { feedViewModel.listState.animateScrollToItem(0) }
                            }
                        }
                    },
                    icon = {
                        val icon = when (tab) {
                            HomeTab -> Icons.Default.Home
                            MessageTab -> Icons.Default.Notifications
                            ProfileTab -> Icons.Default.Person
                            else -> Icons.Default.Error
                        }

                        BadgedBox(
                            badge = {
                                if (tab == MessageTab) UnreadBadge(messageViewModel.uiState.unread)
                            },
                        ) {
                            Icon(icon, null)
                        }
                    },
                    label = {
                        val labelText = when (tab) {
                            HomeTab -> stringResource(R.string.nav_home)
                            MessageTab -> stringResource(R.string.nav_messages)
                            ProfileTab -> stringResource(R.string.nav_profile)
                            else -> "Unknown"
                        }
                        Text(labelText)
                    },
                    alwaysShowLabel = false,
                )
            }
        }

        // After the NavigationBar, so the sheet covers it.
        NegativeFeedbackSheet(
            state = feedViewModel.feedbackState,
            onSubmit = feedViewModel::submitFeedback,
            onDismissRequest = feedViewModel::dismissFeedback,
            onRetry = feedViewModel::retryFeedback,
        )

        val feedbackToast = feedViewModel.feedbackToast
        val toastText = feedbackToast?.let { feedbackToastText(it) }
        val feedbackError = feedViewModel.feedbackError
        val feedbackErrorText = feedbackError?.uiMessage

        LaunchedEffect(toastText) {
            if (toastText != null) {
                Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                feedViewModel.consumeFeedbackToast()
            }
        }

        LaunchedEffect(feedbackError) {
            if (feedbackErrorText != null) {
                Toast.makeText(context, feedbackErrorText, Toast.LENGTH_SHORT).show()
                feedViewModel.consumeFeedbackError()
            }
        }
    }
}
