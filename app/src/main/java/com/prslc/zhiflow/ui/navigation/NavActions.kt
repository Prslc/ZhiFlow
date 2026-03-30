package com.prslc.zhiflow.ui.navigation

import android.content.Context
import android.widget.Toast
import androidx.navigation.NavController

class NavigatorAction(
    private val navController: NavController,
    private val context: Context
) {
    object ContentTypes {
        const val ANSWER = "answer"
        const val ARTICLE = "article"
    }

    fun navigateToContent(id: String, type: String) {
        when (type.lowercase()) {
            ContentTypes.ANSWER -> navController.navigate(AnswerDetail(id))
            ContentTypes.ARTICLE -> navController.navigate(ArticleDetail(id))
            else -> showToast("暂不支持的类型: $type")
        }
    }
    fun navigateToSettings() {
        navController.navigate(Settings)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}