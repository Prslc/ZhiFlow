package com.prslc.zhiflow.ui.navigation

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.model.search.SearchTab
import com.prslc.zhiflow.data.model.user.PeopleListKind
import kotlinx.serialization.Serializable

@Immutable
@Serializable
object MainContainer

@Immutable
@Serializable
object Settings

/** The settings one layer down, which is where what a content page draws is chosen. */
@Immutable
@Serializable
object ReadingSettings

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

/** A column, which is addressed by its token (`c_1867989249418211328`) rather than by a number. */
@Immutable
@Serializable
data class ColumnDetail(val urlToken: String)

@Immutable
@Serializable
data class PeopleDetail(val urlToken: String)

/**
 * One of an account's lists of people: who it follows, or who follows it.
 *
 * The list rides along as the identifier [PeopleListKind] names it by, for the reason [Search]'s
 * tab does.
 */
@Immutable
@Serializable
data class PeopleList(val userId: String, val kind: String)

@Immutable
@Serializable
object HomeTab

/** The bottom bar's middle tab: what the reader has been told, and what they have been sent. */
@Immutable
@Serializable
object MessageTab

/** One list of notifications. The title is the server's name for it, which is what heads the page. */
@Immutable
@Serializable
data class NotificationCategory(val type: String, val title: String)

/**
 * How a conversation is said to have been opened when the reader named someone rather than opened
 * one they already had: the value the client's own sends for a conversation started from a name.
 */
const val CHAT_SOURCE_SEED = "seed_chat"

/** The page a conversation is started from: name someone, and the server hands back who it found. */
@Immutable
@Serializable
object ChatInvite

/**
 * One conversation, opened by the user hash of the person at the other end.
 *
 * [sourceType] rides along because writing into a conversation tells the server how the page was
 * opened, and that is the address's to say rather than this app's to work out.
 */
@Immutable
@Serializable
data class Chat(val peerId: String, val sourceType: String? = null)

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
