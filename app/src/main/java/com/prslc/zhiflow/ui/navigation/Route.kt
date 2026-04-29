package com.prslc.zhiflow.ui.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
object MainContainer

@Stable
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

@Stable
@Serializable
object HomeTab

@Stable
@Serializable
object DebugTab

@Stable
@Serializable
object ProfileTab