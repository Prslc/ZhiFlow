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