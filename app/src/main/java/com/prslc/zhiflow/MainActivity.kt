package com.prslc.zhiflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.prslc.zhiflow.ui.AnswerDetail
import com.prslc.zhiflow.ui.DebugTab
import com.prslc.zhiflow.ui.HomeTab
import com.prslc.zhiflow.ui.MainContainer
import com.prslc.zhiflow.ui.ProfileTab
import com.prslc.zhiflow.ui.Settings
import com.prslc.zhiflow.ui.screen.AnswerScreen
import com.prslc.zhiflow.ui.screen.DebugScreen
import com.prslc.zhiflow.ui.theme.ZhiFlowTheme
import com.prslc.zhiflow.ui.screen.FeedScreen
import com.prslc.zhiflow.ui.screen.ProfileScreen
import com.prslc.zhiflow.ui.screen.SettingsScreen
import com.prslc.zhiflow.ui.viewmodel.FeedViewModel
import com.prslc.zhiflow.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZhiFlowTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = MainContainer
                ) {
                    composable<MainContainer> {
                        MainScreen(
                            onNavigateToAnswer = { id -> navController.navigate(AnswerDetail(id)) },
                            onNavigateToSettings = { navController.navigate(Settings) }
                        )
                    }

                    composable<AnswerDetail> { backStackEntry ->
                        val route: AnswerDetail = backStackEntry.toRoute()
                        AnswerScreen(answerId = route.id, onBack = { navController.popBackStack() })
                    }

                    composable<Settings> {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    onNavigateToAnswer: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val tabs = listOf(HomeTab, DebugTab, ProfileTab)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    val feedViewModel: FeedViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    NavigationSuiteScaffold(
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
                    label = { Text(tab.javaClass.simpleName.removeSuffix("Tab")) },
                    alwaysShowLabel = false
                )
            }
        }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) { pageIndex ->
            when (tabs[pageIndex]) {
                HomeTab -> FeedScreen(
                    viewModel = feedViewModel,
                    onItemClick = onNavigateToAnswer
                )
                DebugTab -> DebugScreen(
                    onNavigateToAnswer = onNavigateToAnswer
                )
                ProfileTab -> ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}