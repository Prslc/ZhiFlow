package com.prslc.zhiflow.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.prslc.zhiflow.MainScreen
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.ui.page.content.ContentDetailScreen
import com.prslc.zhiflow.ui.page.people.PeopleScreen
import com.prslc.zhiflow.ui.page.question.QuestionDetailScreen
import com.prslc.zhiflow.ui.page.profile.SettingsScreen

fun NavGraphBuilder.contentGraph(navController: NavHostController) {
    composable<MainContainer> {
        MainScreen()
    }

    composable<AnswerDetail> { backStackEntry ->
        val route: AnswerDetail = backStackEntry.toRoute()
        ContentDetailScreen(
            id = route.id,
            contentType = ContentType.ANSWER,
            onBack = { navController.popBackStack() })
    }

    composable<ArticleDetail> { backStackEntry ->
        val route: ArticleDetail = backStackEntry.toRoute()
        ContentDetailScreen(
            id = route.id,
            contentType = ContentType.ARTICLE,
            onBack = { navController.popBackStack() })
    }

    composable<PinDetail> { backStackEntry ->
        val route: PinDetail = backStackEntry.toRoute()
        ContentDetailScreen(
            id = route.id,
            contentType = ContentType.PIN,
            onBack = { navController.popBackStack() })
    }

    composable<QuestionDetail> { backStackEntry ->
        val route: QuestionDetail = backStackEntry.toRoute()
        QuestionDetailScreen(
            id = route.id,
            onBack = { navController.popBackStack() }
        )
    }

    composable<PeopleDetail> { backStackEntry ->
        val route: PeopleDetail = backStackEntry.toRoute()
        PeopleScreen(
            urlToken = route.urlToken,
            onBack = { navController.popBackStack() }
        )
    }

    composable<Settings> {
        SettingsScreen(onBack = { navController.popBackStack() })
    }
}