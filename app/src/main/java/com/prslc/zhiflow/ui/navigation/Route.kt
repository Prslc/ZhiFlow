package com.prslc.zhiflow.ui.navigation

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
object MainContainer

@Immutable
@Serializable
object Settings

@Immutable
@Serializable
data class AnswerDetail(val id: String)

@Immutable
@Serializable
data class ArticleDetail(val id: String)

@Immutable
@Serializable
data class PinDetail(val id: String)

@Immutable
@Serializable
data class QuestionDetail(val id: String)

@Immutable
@Serializable
data class PeopleDetail(val urlToken: String)

@Immutable
@Serializable
object HomeTab

@Immutable
@Serializable
object DebugTab

@Immutable
@Serializable
object ProfileTab

@Immutable
@Serializable
object ReadHistory
