package com.prslc.zhiflow

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.exception.uiMessage
import com.prslc.zhiflow.core.preference.SettingsStore
import com.prslc.zhiflow.core.preference.ThemeMode
import com.prslc.zhiflow.core.utils.compose.rememberHideOnScroll
import com.prslc.zhiflow.data.session.SessionManager
import com.prslc.zhiflow.ui.component.LocalDataSaver
import com.prslc.zhiflow.ui.component.widget.ImageLightboxHost
import com.prslc.zhiflow.ui.navigation.HomeTab
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.navigation.MainContainer
import com.prslc.zhiflow.ui.navigation.Navigator
import com.prslc.zhiflow.ui.navigation.ProfileTab
import com.prslc.zhiflow.ui.navigation.contentGraph
import com.prslc.zhiflow.ui.page.debug.DebugScreen
import com.prslc.zhiflow.ui.page.feed.FeedScreen
import com.prslc.zhiflow.ui.page.feed.FeedViewModel
import com.prslc.zhiflow.ui.page.feed.NegativeFeedbackSheet
import com.prslc.zhiflow.ui.page.feed.feedbackToastText
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
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val context = LocalContext.current
                    val navController = rememberNavController()
                    val uriHandler = LocalUriHandler.current
                    val navigator = remember(navController) {
                        Navigator(
                            navController = navController,
                            context = context,
                            uriHandler = uriHandler,
                        )
                    }

                    // Sits outside the NavHost, so it stays composed whichever screen is on top.
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
                        NavHost(
                            navController = navController,
                            startDestination = MainContainer,
                            enterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                )
                            },
                            exitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 5 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                ) + fadeOut(
                                    targetAlpha = 0f,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                )
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { -it / 5 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                ) + fadeIn(
                                    initialAlpha = 0f,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                )
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                )
                            },
                            // Runs during the back gesture, where the pop* pair above does not --
                            // the two have to match or the handoff jumps on release.
                            predictivePopEnterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { -it / 5 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                ) + fadeIn(
                                    initialAlpha = 0f,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                )
                            },
                            predictivePopExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                )
                            },
                        ) {
                            contentGraph(navController)
                        }
                        ImageLightboxHost()
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val feedViewModel: FeedViewModel = koinViewModel()

    val tabs = listOf(HomeTab, ProfileTab)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    val bottomBar = rememberHideOnScroll()

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .nestedScroll(bottomBar.connection),
        ) { pageIndex ->
            when (tabs[pageIndex]) {
                HomeTab -> FeedScreen(
                    onItemClick = { id, type -> navigator.navigateToContent(id, type) },
                    onItemLongClick = feedViewModel::openFeedback,
                    onSearch = navigator::navigateToSearch,
                )

                ProfileTab -> ProfileScreen(
                    onNavigateToHistory = navigator::navigateToReadHistory,
                    onNavigateToComments = { },
                    onNavigateToLikes = { },
                    onNavigateToCollections = navigator::navigateToCollectionContents,
                    onNavigateToFollows = { },
                    onNavigateToSettings = navigator::navigateToSettings,
                )
            }
        }

        AnimatedVisibility(
            visible = bottomBar.visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
        ) {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
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
                            Icon(
                                when (tab) {
                                    HomeTab -> Icons.Default.Home
                                    ProfileTab -> Icons.Default.Person
                                    else -> Icons.Default.Error
                                },
                                null,
                            )
                        },
                        label = {
                            val labelText = when (tab) {
                                HomeTab -> stringResource(R.string.nav_home)
                                ProfileTab -> stringResource(R.string.nav_profile)
                                else -> "Unknown"
                            }
                            Text(labelText)
                        },
                        alwaysShowLabel = false,
                    )
                }
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
