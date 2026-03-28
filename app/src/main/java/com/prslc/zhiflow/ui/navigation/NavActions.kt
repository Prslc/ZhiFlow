package com.prslc.zhiflow.ui.navigation

import android.content.Context
import android.widget.Toast
import androidx.navigation.NavController

class NavigatorAction(
    private val navController: NavController,
    private val context: Context
) {
    fun navigateToContent(id: String, type: String) {
        when (type) {
            "answer" -> navController.navigate(AnswerDetail(id))
            "article" -> showToast("文章阅读功能开发中...")
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