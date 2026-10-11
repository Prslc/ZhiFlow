package com.prslc.zhiflow.data.model.content

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class StructuredContent(
    val segments: List<Segment>,
    val paging: String? = null,
)

@Immutable
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

@Immutable
@Serializable
data class Paragraph(
    val text: String = "",
    val marks: List<Mark> = emptyList(),
    val pid: String? = null,
)

@Immutable
@Serializable
data class ReferenceBlock(
    val items: List<ReferenceItem>,
)

@Immutable
@Serializable
data class ReferenceItem(
    val text: String,
    @SerialName("indent_level") val indentLevel: Int = 1,
    val marks: List<Mark> = emptyList(),
)

@Immutable
@Serializable
data class Heading(
    val text: String, // title style
    val level: Int,
    val marks: List<Mark> = emptyList(),
)

@Immutable
@Serializable
data class Mark(
    val type: String, // bold, italic, code, link, entity_word, formula, reference, seg_like
    @SerialName("start_index") val start: Int,
    @SerialName("end_index") val end: Int,
    val link: Link? = null,
    @SerialName("entity_word") val entityWord: EntityWord? = null,
    val formula: Formula? = null,
    val reference: Reference? = null,
    @SerialName("seg_like") val segLike: SegmentLike? = null,
    @SerialName("master_seg_like") val masterSegLike: SegmentLike? = null,
)

@Immutable
@Serializable
data class Blockquote(
    val text: String,
    val marks: List<Mark> = emptyList(),
    val pid: String? = null,
)

@Immutable
@Serializable
data class CodeBlock(
    val content: String,
    val language: String? = "text",
)

@Immutable
@Serializable
data class ListNode(
    val type: String, // ordered, unordered
    val items: List<ListItem>,
)

@Immutable
@Serializable
data class ListItem(
    val text: String,
    @SerialName("indent_level") val indentLevel: Int = 1,
    val marks: List<Mark> = emptyList(),
)

@Immutable
@Serializable
data class Table(
    val cells: List<String>,
    @SerialName("column_count") val columnCount: Int,
    @SerialName("row_count") val rowCount: Int,
    @SerialName("head_row") val hasHeadRow: Boolean = true,
    val interlaced: Boolean = false,
)

@Immutable
@Serializable
data class Link(
    val href: String,
    @SerialName("icon_name") val iconName: String? = null,
)

/**
 * A phrase in the body the reader could search for.
 *
 * [type] says what kind of entity it is — `search`, in every capture we have — and [entityId] names
 * it within that kind. [url] is the search it would open; nothing renders it as a link, since this
 * app runs no search of its own.
 */
@Immutable
@Serializable
data class EntityWord(
    val word: String,
    val url: String? = null,
    @SerialName("id") val entityId: String? = null,
    val type: String? = null,
)

@Immutable
@Serializable
data class Formula(
    val content: String, // LaTeX
    @SerialName("img_url") val imgUrl: String? = null,
    val width: Int = 0,
    val height: Int = 0,
)

@Immutable
@Serializable
data class Reference(
    val index: Int,
    val title: String? = null,
    val href: String? = null,
)

@Immutable
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

@Immutable
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
    @SerialName("action_url") val actionUrl: String? = null,
    @SerialName("image_list") val imageList: CardImageList? = null,
)

@Immutable
@Serializable
data class CardImageList(
    val count: Int = 0,
    @SerialName("is_grid") val isGrid: Boolean = true,
    val images: List<CardImageItem> = emptyList(),
)

@Immutable
@Serializable
data class CardImageItem(
    val token: String? = null,
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val suffix: String? = null,
    @SerialName("original_url") val originalUrl: String? = null,
    @SerialName("original_width") val originalWidth: Int? = null,
    @SerialName("original_height") val originalHeight: Int? = null,
)

/**
 * The `extra_info` of the card a body ends with when it was published in a column.
 *
 * It is a string of JSON in a shape of its own, and not the flat one [CardExtraInfo] reads: the
 * things the card draws sit together under [module], and the token naming the column sits beside
 * that. The column's own title is only here — the card carries no title of its own.
 */
@Immutable
@Serializable
data class ColumnCardExtra(
    @SerialName("content_module") val module: ColumnCardModule? = null,
    @SerialName("column_token") val columnToken: String = "",
)

@Immutable
@Serializable
data class ColumnCardModule(
    @SerialName("header_tips") val headerTips: String = "",
    @SerialName("column_title") val columnTitle: String = "",
    @SerialName("column_desc") val columnDesc: String = "",
    @SerialName("column_cover") val columnCover: String = "",
    /** The column's own address in the app's scheme: `zhihu://column/{token}`. */
    @SerialName("action_url") val actionUrl: String = "",
    @SerialName("latest_content") val latestContent: ColumnCardLatest? = null,
)

/** What the column last published, which the card offers as the way in. */
@Immutable
@Serializable
data class ColumnCardLatest(
    val title: String = "",
    @SerialName("title_prefix") val titlePrefix: String = "",
)
