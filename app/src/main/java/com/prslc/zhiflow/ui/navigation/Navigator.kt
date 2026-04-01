package com.prslc.zhiflow.ui.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("NavController not provided")
}

class Navigator(
    private val navController: NavHostController,
    private val context: Context
) {
    fun navigateToContent(id: String, type: String) {
        when (type.lowercase()) {
            "answer" -> navController.navigate(AnswerDetail(id))
            "article" -> navController.navigate(ArticleDetail(id))
            else -> Toast.makeText(context, "未知类型: $type", Toast.LENGTH_SHORT).show()
        }
    }

    fun navigateToSettings() = navController.navigate(Settings)

    fun back() = navController.popBackStack()
}