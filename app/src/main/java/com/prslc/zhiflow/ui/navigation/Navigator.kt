package com.prslc.zhiflow.ui.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.search.SearchTab
import com.prslc.zhiflow.data.model.user.PeopleListKind
import com.prslc.zhiflow.data.remote.parser.LinkDestination
import com.prslc.zhiflow.data.remote.parser.LinkParser

@SuppressLint("ComposeCompositionLocalUsage")
val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("NavController not provided")
}

class Navigator(
    private val navController: NavHostController,
    private val context: Context,
    private val uriHandler: UriHandler,
) {

    /**
     * Navigate to a URL, resolving internal Zhihu routes vs external links.
     *
     * Internal routes (answer, article, question, people, pin) navigate via
     * [NavHostController]; external URLs open in the system browser.
     */
    fun handleUrl(url: String) {
        when (val dest = LinkParser.parse(url)) {
            is LinkDestination.Internal -> {
                navController.navigate(dest.route)
            }

            is LinkDestination.External -> {
                runCatching {
                    uriHandler.openUri(dest.url)
                }.onFailure {
                    Toast.makeText(
                        context,
                        context.getString(R.string.error_unable_to_open_link),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    /**
     * Navigate to a content detail by content type string.
     *
     * @param type Content type: "answer", "article", "pin", or "question"
     */
    fun navigateToContent(id: String, type: String) {
        when (type.lowercase()) {
            "answer" -> navController.navigate(AnswerDetail(id))

            "article" -> navController.navigate(ArticleDetail(id))

            "pin" -> navController.navigate(PinDetail(id))

            "question" -> navController.navigate(QuestionDetail(id))

            "people", "profile" -> navController.navigate(PeopleDetail(id))

            else -> Toast.makeText(
                context,
                context.getString(R.string.error_unknown_nav_type, type),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun navigateToPeople(urlToken: String) {
        navController.navigate(PeopleDetail(urlToken))
    }

    /**
     * Opens one of an account's lists of people.
     *
     * @param userId Whose list it is: the account's hash id.
     * @param kind Which of the two lists.
     */
    fun navigateToPeopleList(userId: String, kind: PeopleListKind) =
        navController.navigate(PeopleList(userId, kind.value))

    fun navigateToSettings() = navController.navigate(Settings)

    fun navigateToAbout() = navController.navigate(About)

    fun navigateToOpenSourceLicense() = navController.navigate(OpenSourceLicense)

    fun navigateToLogin() = navController.navigate(Login)

    fun navigateToReadHistory() = navController.navigate(ReadHistory)

    fun navigateToCollectionContents() = navController.navigate(CollectionContents)

    fun navigateToFeedbackBlockList(route: FeedbackBlockList) = navController.navigate(route)

    fun navigateToHttpLog() = navController.navigate(HttpLog)

    fun navigateToRenderLab() = navController.navigate(RenderLab)

    fun navigateToDebugTools() = navController.navigate(DebugTools)

    /**
     * Opens the search page.
     *
     * @param tab The tab it opens in. The default searches the whole site; the collections page
     *   opens the one scoped to what the reader has saved.
     */
    fun navigateToSearch(tab: SearchTab = SearchTab.GENERAL) =
        navController.navigate(Search(tab.value))

    /**
     * Opens one list of notifications.
     *
     * @param type Which list, in the spelling the message page names it by.
     * @param title What the server calls it, which the page is headed with.
     */
    fun navigateToNotificationCategory(type: String, title: String) =
        navController.navigate(NotificationCategory(type, title))

    /** Opens the page a conversation is started from. */
    fun navigateToChatInvite() = navController.navigate(ChatInvite)

    /**
     * Opens a conversation.
     *
     * @param peerId The user hash of the person at the other end.
     * @param sourceType How the page was opened, when the card that opened it said.
     */
    fun navigateToChat(peerId: String, sourceType: String? = null) =
        navController.navigate(Chat(peerId, sourceType))
}
