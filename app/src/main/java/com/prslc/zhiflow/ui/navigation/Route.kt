package com.prslc.zhiflow.ui.navigation

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.model.search.SearchTab
import kotlinx.serialization.Serializable

@Immutable
@Serializable
object MainContainer

@Immutable
@Serializable
object Settings

@Immutable
@Serializable
object About

@Immutable
@Serializable
object OpenSourceLicense

@Immutable
@Serializable
object Login

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
object DebugTools

/**
 * The search page. The tab rides along so the page can be opened scoped to the reader's own saved
 * content, which is the only way into the collections one.
 *
 * It is carried as the identifier `/search_v3` takes rather than as a [SearchTab]: navigation
 * resolves a type of its own by looking the class up under its own name, which minification does
 * not leave alone.
 */
@Immutable
@Serializable
data class Search(val tab: String)

@Immutable
@Serializable
object ProfileTab

@Immutable
@Serializable
object ReadHistory

@Immutable
@Serializable
object HttpLog

@Immutable
@Serializable
object RenderLab

@Immutable
@Serializable
object CollectionContents

/**
 * The "not interested" topics page. Its parameters are the ones the panel's
 * `zhihu://feedback/block_list` link carries, including the numeric content type.
 */
@Immutable
@Serializable
data class FeedbackBlockList(
    val contentToken: String,
    val contentType: String,
    val feedbackType: String,
)
