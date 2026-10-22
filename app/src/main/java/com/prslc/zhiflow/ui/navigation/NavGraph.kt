package com.prslc.zhiflow.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.prslc.zhiflow.MainScreen
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.model.search.SearchTab
import com.prslc.zhiflow.data.model.user.PeopleListKind
import com.prslc.zhiflow.ui.page.collection.CollectionContentsScreen
import com.prslc.zhiflow.ui.page.column.ColumnScreen
import com.prslc.zhiflow.ui.page.content.ContentDetailScreen
import com.prslc.zhiflow.ui.page.debug.DebugScreen
import com.prslc.zhiflow.ui.page.debug.HttpLogScreen
import com.prslc.zhiflow.ui.page.debug.RenderLabScreen
import com.prslc.zhiflow.ui.page.feed.FeedViewModel
import com.prslc.zhiflow.ui.page.feedback.FeedbackBlockListScreen
import com.prslc.zhiflow.ui.page.history.ReadHistoryScreen
import com.prslc.zhiflow.ui.page.login.LoginScreen
import com.prslc.zhiflow.ui.page.notification.ChatInviteScreen
import com.prslc.zhiflow.ui.page.notification.ChatScreen
import com.prslc.zhiflow.ui.page.notification.NotificationCategoryScreen
import com.prslc.zhiflow.ui.page.people.PeopleListScreen
import com.prslc.zhiflow.ui.page.people.PeopleScreen
import com.prslc.zhiflow.ui.page.profile.AboutScreen
import com.prslc.zhiflow.ui.page.profile.OpenSourceLicenseScreen
import com.prslc.zhiflow.ui.page.profile.ReadingSettingsScreen
import com.prslc.zhiflow.ui.page.profile.SettingsScreen
import com.prslc.zhiflow.ui.page.question.QuestionDetailScreen
import com.prslc.zhiflow.ui.page.search.SearchScreen

/**
 * What each visit draws.
 *
 * The display asks this for an entry whenever the stack changes. A destination reads its own
 * parameters off the route the visit carries, and hands a pop back to [navigator]; nothing here
 * reaches navigation state any other way. The visit's id is the entry's content key, so two visits
 * to one route keep their own saved state and their own ViewModel.
 *
 * @param feedViewModel The card list's ViewModel, held above the display so the block-list page
 *   can drop a card the feed is drawing.
 */
fun contentGraph(
    navigator: Navigator,
    feedViewModel: FeedViewModel,
): (NavKey) -> NavEntry<NavKey> = { key ->
    val visit = key as NavVisit

    fun screen(content: @Composable () -> Unit): NavEntry<NavKey> =
        NavEntry(key = key, contentKey = visit.id) { content() }

    when (val route = visit.route) {
        MainContainer -> screen {
            MainScreen(feedViewModel = feedViewModel)
        }

        is AnswerDetail -> screen {
            ContentDetailScreen(
                id = route.id,
                contentType = ContentType.ANSWER,
                onBack = navigator::goBack,
            )
        }

        is ArticleDetail -> screen {
            ContentDetailScreen(
                id = route.id,
                contentType = ContentType.ARTICLE,
                onBack = navigator::goBack,
            )
        }

        is PinDetail -> screen {
            ContentDetailScreen(
                id = route.id,
                contentType = ContentType.PIN,
                onBack = navigator::goBack,
            )
        }

        is QuestionDetail -> screen {
            QuestionDetailScreen(
                id = route.id,
                onBack = navigator::goBack,
            )
        }

        is ColumnDetail -> screen {
            ColumnScreen(
                urlToken = route.urlToken,
                onBack = navigator::goBack,
            )
        }

        is PeopleDetail -> screen {
            PeopleScreen(
                urlToken = route.urlToken,
                onBack = navigator::goBack,
            )
        }

        is PeopleList -> screen {
            PeopleListScreen(
                userId = route.userId,
                kind = PeopleListKind.from(route.kind),
                onBack = navigator::goBack,
            )
        }

        Settings -> screen {
            SettingsScreen(
                onBack = navigator::goBack,
            )
        }

        ReadingSettings -> screen {
            ReadingSettingsScreen(
                onBack = navigator::goBack,
            )
        }

        About -> screen {
            AboutScreen(
                onBack = navigator::goBack,
            )
        }

        OpenSourceLicense -> screen {
            OpenSourceLicenseScreen(
                onBack = navigator::goBack,
            )
        }

        Login -> screen {
            LoginScreen(onSignedIn = navigator::goBack)
        }

        ReadHistory -> screen {
            ReadHistoryScreen(
                onBack = navigator::goBack,
            )
        }

        CollectionContents -> screen {
            CollectionContentsScreen(
                onBack = navigator::goBack,
            )
        }

        HttpLog -> screen {
            HttpLogScreen(
                onBack = navigator::goBack,
            )
        }

        RenderLab -> screen {
            RenderLabScreen(
                onBack = navigator::goBack,
            )
        }

        DebugTools -> screen {
            DebugScreen(
                onBack = navigator::goBack,
            )
        }

        is Search -> screen {
            SearchScreen(
                tab = SearchTab.from(route.tab),
                onBack = navigator::goBack,
            )
        }

        is NotificationCategory -> screen {
            NotificationCategoryScreen(
                type = route.type,
                title = route.title,
                onBack = navigator::goBack,
            )
        }

        ChatInvite -> screen {
            ChatInviteScreen(
                onBack = navigator::goBack,
            )
        }

        is Chat -> screen {
            ChatScreen(
                peerId = route.peerId,
                sourceType = route.sourceType,
                onBack = navigator::goBack,
            )
        }

        is FeedbackBlockList -> screen {
            FeedbackBlockListScreen(
                route = route,
                onBlocked = { contentId ->
                    feedViewModel.removeContent(contentId)
                    navigator.goBack()
                },
                onBack = navigator::goBack,
            )
        }

        else -> error("Unknown route: $route")
    }
}
