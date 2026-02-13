package com.prslc.zhiflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prslc.zhiflow.ui.screen.AnswerScreen
import com.prslc.zhiflow.ui.theme.ZhiFlowTheme
import com.prslc.zhiflow.ui.screen.FeedScreen
import com.prslc.zhiflow.ui.viewmodel.FeedViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZhiFlowTheme {
                ZhiFlowApp()
            }
        }
    }
}

@Composable
fun ZhiFlowApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var currentAnswerId by rememberSaveable { mutableStateOf<String?>(null) }

    val feedViewModel: FeedViewModel = viewModel()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = currentAnswerId,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn())
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(300)
                            ) + fadeOut()
                        )
                        .apply { targetContentZIndex = 1f }
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(300)
                    ) + fadeIn())
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300)
                            ) + fadeOut()
                        )
                        .apply { targetContentZIndex = 0f }
                }
            },
            label = "ScreenTransition"
        ) { targetId ->
            if (targetId != null) {
                AnswerScreen(
                    answerId = targetId,
                    onBack = { currentAnswerId = null },
                )
            } else {
                NavigationSuiteScaffold(
                    navigationSuiteItems = {
                        AppDestinations.entries.forEach { destination ->
                            item(
                                icon = { Icon(destination.icon, null) },
                                label = { Text(stringResource(destination.labelRes)) },
                                selected = destination == currentDestination,
                                alwaysShowLabel = false,
                                onClick = { currentDestination = destination }
                            )
                        }
                    }
                ) {
                    Scaffold { innerPadding ->
                        when (currentDestination) {
                            AppDestinations.HOME -> FeedScreen(
                                innerPadding = innerPadding,
                                viewModel = feedViewModel,
                                onItemClick = { id -> currentAnswerId = id }
                            )

                            AppDestinations.FAVORITES -> Box(Modifier.padding(innerPadding))
                            AppDestinations.PROFILE -> Box(Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(R.string.nav_home, Icons.Default.Home),
    FAVORITES(R.string.nav_favorites, Icons.Default.Favorite),
    PROFILE(R.string.nav_profile, Icons.Default.AccountBox),
}