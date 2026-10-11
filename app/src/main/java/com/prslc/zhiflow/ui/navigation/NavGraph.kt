package com.prslc.zhiflow.ui.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.prslc.zhiflow.MainScreen
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.model.search.SearchTab
import com.prslc.zhiflow.ui.page.collection.CollectionContentsScreen
import com.prslc.zhiflow.ui.page.content.ContentDetailScreen
import com.prslc.zhiflow.ui.page.debug.DebugScreen
import com.prslc.zhiflow.ui.page.debug.HttpLogScreen
import com.prslc.zhiflow.ui.page.debug.RenderLabScreen
import com.prslc.zhiflow.ui.page.feed.FeedViewModel
import com.prslc.zhiflow.ui.page.feedback.FeedbackBlockListScreen
import com.prslc.zhiflow.ui.page.history.ReadHistoryScreen
import com.prslc.zhiflow.ui.page.login.LoginScreen
import com.prslc.zhiflow.ui.page.people.PeopleScreen
import com.prslc.zhiflow.ui.page.pin.PinDetailScreen
import com.prslc.zhiflow.ui.page.profile.AboutScreen
import com.prslc.zhiflow.ui.page.profile.OpenSourceLicenseScreen
import com.prslc.zhiflow.ui.page.profile.SettingsScreen
import com.prslc.zhiflow.ui.page.question.QuestionDetailScreen
import com.prslc.zhiflow.ui.page.search.SearchScreen
import org.koin.androidx.compose.koinViewModel

fun NavGraphBuilder.contentGraph(navController: NavHostController) {
    composable<MainContainer> {
        MainScreen()
    }

    composable<AnswerDetail> { backStackEntry ->
        val route: AnswerDetail = backStackEntry.toRoute()
        ContentDetailScreen(
            id = route.id,
            contentType = ContentType.ANSWER,
            onBack = { navController.popBackStack() },
        )
    }

    composable<ArticleDetail> { backStackEntry ->
        val route: ArticleDetail = backStackEntry.toRoute()
        ContentDetailScreen(
            id = route.id,
            contentType = ContentType.ARTICLE,
            onBack = { navController.popBackStack() },
        )
    }

    composable<PinDetail> { backStackEntry ->
        val route: PinDetail = backStackEntry.toRoute()
        PinDetailScreen(
            id = route.id,
            onBack = { navController.popBackStack() },
        )
    }

    composable<QuestionDetail> { backStackEntry ->
        val route: QuestionDetail = backStackEntry.toRoute()
        QuestionDetailScreen(
            id = route.id,
            onBack = { navController.popBackStack() },
        )
    }

    composable<PeopleDetail> { backStackEntry ->
        val route: PeopleDetail = backStackEntry.toRoute()
        PeopleScreen(
            urlToken = route.urlToken,
            onBack = { navController.popBackStack() },
        )
    }

    composable<Settings> {
        SettingsScreen(
            onBack = { navController.popBackStack() },
        )
    }

    composable<About> {
        AboutScreen(
            onBack = { navController.popBackStack() },
        )
    }

    composable<OpenSourceLicense> {
        OpenSourceLicenseScreen(
            onBack = { navController.popBackStack() },
        )
    }

    composable<Login> {
        LoginScreen(onSignedIn = { navController.popBackStack() })
    }

    composable<ReadHistory> {
        ReadHistoryScreen(
            onBack = { navController.popBackStack() },
        )
    }

    composable<CollectionContents> {
        CollectionContentsScreen(
            onBack = { navController.popBackStack() },
        )
    }

    composable<HttpLog> {
        HttpLogScreen(
            onBack = { navController.popBackStack() },
        )
    }

    composable<RenderLab> {
        RenderLabScreen(
            onBack = { navController.popBackStack() },
        )
    }

    composable<DebugTools> {
        DebugScreen(
            onBack = { navController.popBackStack() },
        )
    }

    composable<Search> { backStackEntry ->
        val route: Search = backStackEntry.toRoute()
        SearchScreen(
            tab = SearchTab.from(route.tab),
            onBack = { navController.popBackStack() },
        )
    }

    composable<FeedbackBlockList> { backStackEntry ->
        val route: FeedbackBlockList = backStackEntry.toRoute()
        val mainEntry = remember(backStackEntry) {
            // The card list belongs to MainScreen's ViewModel: reach that instance rather than
            // letting this destination resolve one of its own.
            navController.getBackStackEntry<MainContainer>()
        }
        val feedViewModel: FeedViewModel = koinViewModel(viewModelStoreOwner = mainEntry)

        FeedbackBlockListScreen(
            route = route,
            onBlocked = { contentId ->
                feedViewModel.removeContent(contentId)
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() },
        )
    }
}
