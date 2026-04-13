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

    fun handleUrl(url: String, contentType: String? = null) {
        when (val dest = LinkParser.parse(url, contentType)) {
            is LinkDestination.Internal -> {
                navigateToContent(dest.id, dest.type)
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

    fun navigateToContent(id: String, type: String) {
        when (type.lowercase()) {
            "answer" -> navController.navigate(AnswerDetail(id))
            "article" -> navController.navigate(ArticleDetail(id))
            else -> Toast.makeText(context, "Unknown type: $type", Toast.LENGTH_SHORT).show()
        }
    }

    fun navigateToSettings() = navController.navigate(Settings)
}