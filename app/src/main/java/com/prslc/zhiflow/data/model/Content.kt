package com.prslc.zhiflow.data.model

@Stable
interface ZhihuContent {
    val id: String
    val author: AnswerAuthor
    val reaction: Reaction?
    val contentEnd: ContentEndInfo?
    val structuredContent: StructuredContent
    val displayTitle: String
}