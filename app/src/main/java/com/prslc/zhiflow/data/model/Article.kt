package com.prslc.zhiflow.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val id: String,
    val type: String = "article",
    val author: AnswerAuthor,
    val header: Header? = null,
    val reaction: Reaction,
    val excerpt: String? = null,
    @SerialName("content_end_info") val contentEnd: ContentEndInfo? = null,
    @SerialName("structured_content") val structuredContent: StructuredContent
)

@Serializable
data class Heading(
    val text: String,   // title style
    val level: Int,
    val marks: List<Mark> = emptyList()
)

@Serializable
data class Mark(
    val type: String, // bold, italic, code, link, entity_word, formula, reference
    @SerialName("start_index") val start: Int,
    @SerialName("end_index") val end: Int,
    val link: Link? = null,
    @SerialName("entity_word") val entityWord: EntityWord? = null,
    val formula: Formula? = null,
    val reference: Reference? = null
)

@Serializable
data class Blockquote(
    val text: String,
    val marks: List<Mark> = emptyList()
)

@Serializable
data class CodeBlock(
    val content: String,
    val language: String? = "text"
)

@Serializable
data class ListNode(
    val type: String, // ordered, unordered
    val items: List<ListItem>
)

@Serializable
data class ListItem(
    val text: String,
    @SerialName("indent_level") val indentLevel: Int = 1,
    val marks: List<Mark> = emptyList()
)

@Serializable
data class Table(
    val cells: List<String>,
    @SerialName("column_count") val columnCount: Int,
    @SerialName("row_count") val rowCount: Int,
    @SerialName("head_row") val hasHeadRow: Boolean = true
)

@Serializable
data class Link(
    val href: String,
    @SerialName("icon_name") val iconName: String? = null
)

@Serializable
data class EntityWord(
    val word: String,
    val url: String? = null,
    @SerialName("entity_id") val entityId: String? = null
)

@Serializable
data class Formula(
    val content: String, // LaTeX
    @SerialName("img_url") val imgUrl: String? = null,
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class Reference(
    val index: Int,
    val title: String? = null,
    val href: String? = null
)