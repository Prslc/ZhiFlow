package com.prslc.zhiflow.ui.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.UriHandler
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.search.SearchTab
import com.prslc.zhiflow.data.model.user.PeopleListKind
import com.prslc.zhiflow.data.remote.parser.LinkDestination
import com.prslc.zhiflow.data.remote.parser.LinkParser

@SuppressLint("ComposeCompositionLocalUsage")
val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("Navigator not provided")
}

/**
 * What every screen reaches navigation through.
 *
 * The stack is the whole of the navigation state: the display draws whatever is on top of it, and
 * a destination is opened by pushing a visit to its route. There is no controller between the two,
 * so a screen's own navigation is a call on this class rather than a callback threaded down from
 * whatever draws it.
 */
class Navigator(
    private val backStack: NavBackStack<NavKey>,
    private val context: Context,
    private val uriHandler: UriHandler,
) {

    /** The last id handed out, so a stack restored after process death is never renumbered. */
    private var lastId: Long = backStack.maxOfOrNull { (it as? NavVisit)?.id ?: 0L } ?: 0L

    /**
     * Navigate to a URL, resolving internal Zhihu routes vs external links.
     *
     * An internal address is opened as the route it names; external URLs open in the system browser.
     */
    fun handleUrl(url: String) {
        when (val dest = LinkParser.parse(url)) {
            is LinkDestination.Internal -> {
                navigateToContent(dest.id, dest.type)
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
     * @param type Content type: "answer", "article", "pin", "question", or "column"
     */
    fun navigateToContent(id: String, type: String) {
        when (type.lowercase()) {
            "answer" -> navigate(AnswerDetail(id))

            "article" -> navigate(ArticleDetail(id))

            "pin" -> navigate(PinDetail(id))

            "question" -> navigate(QuestionDetail(id))

            "column" -> navigate(ColumnDetail(id))

            "people", "profile" -> navigate(PeopleDetail(id))

            else -> Toast.makeText(
                context,
                context.getString(R.string.error_unknown_nav_type, type),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun navigateToPeople(urlToken: String) {
        navigate(PeopleDetail(urlToken))
    }

    /**
     * Opens a column.
     *
     * @param urlToken The column's token, as its links name it.
     */
    fun navigateToColumn(urlToken: String) = navigate(ColumnDetail(urlToken))

    /**
     * Opens one of an account's lists of people.
     *
     * @param userId Whose list it is: the account's hash id.
     * @param kind Which of the two lists.
     */
    fun navigateToPeopleList(userId: String, kind: PeopleListKind) =
        navigate(PeopleList(userId, kind.value))

    fun navigateToSettings() = navigate(Settings)

    fun navigateToReadingSettings() = navigate(ReadingSettings)

    fun navigateToAbout() = navigate(About)

    fun navigateToOpenSourceLicense() = navigate(OpenSourceLicense)

    fun navigateToLogin() = navigate(Login)

    fun navigateToReadHistory() = navigate(ReadHistory)

    fun navigateToCollectionContents() = navigate(CollectionContents)

    fun navigateToFeedbackBlockList(route: FeedbackBlockList) = navigate(route)

    fun navigateToHttpLog() = navigate(HttpLog)

    fun navigateToRenderLab() = navigate(RenderLab)

    fun navigateToDebugTools() = navigate(DebugTools)

    /**
     * Opens the search page.
     *
     * @param tab The tab it opens in. The default searches the whole site; the collections page
     *   opens the one scoped to what the reader has saved.
     */
    fun navigateToSearch(tab: SearchTab = SearchTab.GENERAL) =
        navigate(Search(tab.value))

    /**
     * Opens one list of notifications.
     *
     * @param type Which list, in the spelling the message page names it by.
     * @param title What the server calls it, which the page is headed with.
     */
    fun navigateToNotificationCategory(type: String, title: String) =
        navigate(NotificationCategory(type, title))

    /** Opens the page a conversation is started from. */
    fun navigateToChatInvite() = navigate(ChatInvite)

    /**
     * Opens a conversation.
     *
     * @param peerId The user hash of the person at the other end.
     * @param sourceType How the page was opened, when the card that opened it said.
     */
    fun navigateToChat(peerId: String, sourceType: String? = null) =
        navigate(Chat(peerId, sourceType))

    /** Pops the page on top. The bottom of the stack is the app's root and stays. */
    fun goBack() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    private fun navigate(route: NavKey) {
        // Two taps on one row arrive before the first push has been drawn, and the second would
        // stack the page the first one opened.
        if ((backStack.lastOrNull() as? NavVisit)?.route == route) return
        lastId += 1
        backStack.add(NavVisit(id = lastId, route = route))
    }
}
