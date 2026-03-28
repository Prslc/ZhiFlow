package com.prslc.zhiflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.prslc.zhiflow.ui.navigation.AnswerDetail
import com.prslc.zhiflow.ui.navigation.DebugTab
import com.prslc.zhiflow.ui.navigation.HomeTab
import com.prslc.zhiflow.ui.navigation.MainContainer
import com.prslc.zhiflow.ui.navigation.NavigatorAction
import com.prslc.zhiflow.ui.navigation.ProfileTab
import com.prslc.zhiflow.ui.navigation.Settings
import com.prslc.zhiflow.ui.screen.AnswerScreen
import com.prslc.zhiflow.ui.screen.DebugScreen
import com.prslc.zhiflow.ui.screen.FeedScreen
import com.prslc.zhiflow.ui.screen.ProfileScreen
import com.prslc.zhiflow.ui.screen.SettingsScreen
import com.prslc.zhiflow.ui.theme.ZhiFlowTheme
import com.prslc.zhiflow.ui.viewmodel.FeedViewModel
import com.prslc.zhiflow.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ZhiFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    val navigator = remember(navController, context) {
                        NavigatorAction(
                            navController,
                            context
                        )
                    }

                    NavHost(
                        navController = navController,
                        startDestination = MainContainer,
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 5 },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 5 },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(600, easing = FastOutSlowInEasing)
                            )
                        }
                    ) {
                        composable<MainContainer> {
                            MainScreen(
                                onNavigateToContent = navigator::navigateToContent,
                                onNavigateToSettings = navigator::navigateToSettings
                            )
                        }

                        composable<AnswerDetail> { backStackEntry ->
                            val route: AnswerDetail = backStackEntry.toRoute()
                            AnswerScreen(
                                answerId = route.id,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<Settings> {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    onNavigateToContent: (String, String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val tabs = listOf(HomeTab, DebugTab, ProfileTab)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    val feedViewModel: FeedViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    NavigationSuiteScaffold(
        layoutType = NavigationSuiteType.NavigationBar,
        navigationSuiteItems = {
            tabs.forEachIndexed { index, tab ->
                item(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    icon = {
                        Icon(
                            when(tab) {
                                HomeTab -> Icons.Default.Home
                                DebugTab -> Icons.Default.BugReport
                                ProfileTab -> Icons.Default.Person
                                else -> Icons.Default.Error
                            },
                            null
                        )
                    },
                    label = {
                        val labelText = when(tab) {
                            HomeTab -> stringResource(R.string.nav_home)
                            DebugTab -> stringResource(R.string.nav_debug)
                            ProfileTab -> stringResource(R.string.nav_profile)
                            else -> "Unknown"
                        }
                        Text(labelText)
                    },
                    alwaysShowLabel = false
                )
            }
        }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) { pageIndex ->
            when (tabs[pageIndex]) {
                HomeTab -> FeedScreen(
                    viewModel = feedViewModel,
                    onItemClick = onNavigateToContent
                )
                DebugTab -> DebugScreen(
                    onNavigateToAnswer = onNavigateToContent
                )
                ProfileTab -> ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}