package com.prslc.zhiflow.navigation

import kotlinx.serialization.Serializable

@Serializable object MainContainer
@Serializable object Settings
@Serializable data class AnswerDetail(val id: String)
@Serializable data class ArticleDetail(val id: String)

@Serializable object HomeTab
@Serializable object DebugTab
@Serializable object ProfileTab