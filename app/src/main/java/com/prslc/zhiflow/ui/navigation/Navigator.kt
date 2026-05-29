package com.prslc.zhiflow.ui.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.prslc.zhiflow.parser.LinkDestination
import com.prslc.zhiflow.parser.LinkParser

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("NavController not provided")
}

class Navigator(
    private val navController: NavHostController,
    private val context: Context,
    private val uriHandler: UriHandler
) {

    /**
     * Navigate to a URL, resolving internal Zhihu routes vs external links.
     *
     * Internal routes (answer, article, question, people, pin) navigate via
     * [NavHostController]; external URLs open in the system browser.
     */
    fun handleUrl(url: String, contentType: String? = null) {
        when (val dest = LinkParser.parse(url, contentType)) {
            is LinkDestination.Internal -> {
                navController.navigate(dest.route)
            }

            is LinkDestination.External -> {
                runCatching {
                    uriHandler.openUri(dest.url)
                }.onFailure {
                    Toast.makeText(context, "Unable to open link.", Toast.LENGTH_SHORT).show()
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
            else -> Toast.makeText(context, "Unknown type: $type", Toast.LENGTH_SHORT).show()
        }
    }

    fun navigateToPeople(urlToken: String) {
        navController.navigate(PeopleDetail(urlToken))
    }

    fun navigateToSettings() = navController.navigate(Settings)
}
