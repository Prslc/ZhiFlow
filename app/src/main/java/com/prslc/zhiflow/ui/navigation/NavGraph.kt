package com.prslc.zhiflow.ui.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
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
 * What each route draws.
 *
 * The display asks this for an entry whenever the stack changes. A destination reads its own
 * parameters off the route it is handed, and hands a pop back to [navigator]; nothing here reaches
 * navigation state any other way.
 *
 * @param feedViewModel The card list's ViewModel, held above the display so the block-list page
 *   can drop a card the feed is drawing.
 */
fun contentGraph(
    navigator: Navigator,
    feedViewModel: FeedViewModel,
): (NavKey) -> NavEntry<NavKey> = entryProvider {
    entry<MainContainer> {
        MainScreen(feedViewModel = feedViewModel)
    }

    entry<AnswerDetail> { route ->
        ContentDetailScreen(
            id = route.id,
            contentType = ContentType.ANSWER,
            onBack = navigator::goBack,
        )
    }

    entry<ArticleDetail> { route ->
        ContentDetailScreen(
            id = route.id,
            contentType = ContentType.ARTICLE,
            onBack = navigator::goBack,
        )
    }

    entry<PinDetail> { route ->
        ContentDetailScreen(
            id = route.id,
            contentType = ContentType.PIN,
            onBack = navigator::goBack,
        )
    }

    entry<QuestionDetail> { route ->
        QuestionDetailScreen(
            id = route.id,
            onBack = navigator::goBack,
        )
    }

    entry<ColumnDetail> { route ->
        ColumnScreen(
            urlToken = route.urlToken,
            onBack = navigator::goBack,
        )
    }

    entry<PeopleDetail> { route ->
        PeopleScreen(
            urlToken = route.urlToken,
            onBack = navigator::goBack,
        )
    }

    entry<PeopleList> { route ->
        PeopleListScreen(
            userId = route.userId,
            kind = PeopleListKind.from(route.kind),
            onBack = navigator::goBack,
        )
    }

    entry<Settings> {
        SettingsScreen(
            onBack = navigator::goBack,
        )
    }

    entry<ReadingSettings> {
        ReadingSettingsScreen(
            onBack = navigator::goBack,
        )
    }

    entry<About> {
        AboutScreen(
            onBack = navigator::goBack,
        )
    }

    entry<OpenSourceLicense> {
        OpenSourceLicenseScreen(
            onBack = navigator::goBack,
        )
    }

    entry<Login> {
        LoginScreen(onSignedIn = navigator::goBack)
    }

    entry<ReadHistory> {
        ReadHistoryScreen(
            onBack = navigator::goBack,
        )
    }

    entry<CollectionContents> {
        CollectionContentsScreen(
            onBack = navigator::goBack,
        )
    }

    entry<HttpLog> {
        HttpLogScreen(
            onBack = navigator::goBack,
        )
    }

    entry<RenderLab> {
        RenderLabScreen(
            onBack = navigator::goBack,
        )
    }

    entry<DebugTools> {
        DebugScreen(
            onBack = navigator::goBack,
        )
    }

    entry<Search> { route ->
        SearchScreen(
            tab = SearchTab.from(route.tab),
            onBack = navigator::goBack,
        )
    }

    entry<NotificationCategory> { route ->
        NotificationCategoryScreen(
            type = route.type,
            title = route.title,
            onBack = navigator::goBack,
        )
    }

    entry<ChatInvite> {
        ChatInviteScreen(
            onBack = navigator::goBack,
        )
    }

    entry<Chat> { route ->
        ChatScreen(
            peerId = route.peerId,
            sourceType = route.sourceType,
            onBack = navigator::goBack,
        )
    }

    entry<FeedbackBlockList> { route ->
        FeedbackBlockListScreen(
            route = route,
            onBlocked = { contentId ->
                feedViewModel.removeContent(contentId)
                navigator.goBack()
            },
            onBack = navigator::goBack,
        )
    }
}
