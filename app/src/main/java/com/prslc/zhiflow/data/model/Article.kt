package com.prslc.zhiflow.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZhihuArticle(
    override val id: String,
    @SerialName("header") val header: ArticleHeader? = null,
    override val author: AnswerAuthor,
    override val reaction: Reaction,
    @SerialName("content_end_info") override val contentEnd: ContentEndInfo? = null,
    @SerialName("structured_content") override val structuredContent: StructuredContent,
    val type: String = "article"
) : ZhihuContent {
    override val displayTitle: String
        get() = header?.text ?: "No Title"
}

@Serializable
data class ArticleHeader(
    @SerialName("text") val text: String = ""
)