package com.prslc.zhiflow.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StructuredContent(
    val segments: List<Segment>,
    val paging: String? = null
)

@Serializable
data class Segment(
    val type: String, // paragraph, heading, blockquote, code_block, list_node, table, hr, formula...
    val id: String? = null,
    val paragraph: Paragraph? = null,
    val heading: Heading? = null,
    val blockquote: Blockquote? = null,
    @SerialName("code_block") val codeBlock: CodeBlock? = null,
    @SerialName("list_node") val listNode: ListNode? = null,
    val table: Table? = null,
    val image: ZhihuImage? = null,
    @SerialName("reference_block") val referenceBlock: ReferenceBlock? = null,
    val card: Card? = null,
)

@Serializable
data class Paragraph(
    val text: String = "",
    val marks: List<Mark> = emptyList(),
    val pid: String? = null
)

@Serializable
data class ReferenceBlock(
    val items: List<ReferenceItem>
)

@Serializable
data class ReferenceItem(
    val text: String,
    @SerialName("indent_level") val indentLevel: Int = 1,
    val marks: List<Mark> = emptyList()
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

@Serializable
data class Card(
    @SerialName("card_type") val cardType: String, // link-card, free_column_card, etc.
    @SerialName("content_type") val contentType: String? = null, // ANSWER, QUESTION
    val title: String? = null,
    val url: String? = null,
    @SerialName("url_token") val urlToken: String? = null,
    @SerialName("extra_info") val extraInfo: String? = null,
    val cover: String? = null,
)

@Serializable
data class CardExtraInfo(
    val title: String? = null,
    val url: String? = null,
    val cover: String? = null,
    val desc: String? = null,
    @SerialName("data-content-token") val contentToken: String? = null,
    @SerialName("data-content-type") val contentType: String? = null,
    @SerialName("column_title") val columnTitle: String? = null,
    @SerialName("column_desc") val columnDesc: String? = null,
    @SerialName("action_url") val actionUrl: String? = null
)