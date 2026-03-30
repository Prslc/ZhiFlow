package com.prslc.zhiflow.data.model

enum class ContentType(
    val type: String,
    val apiPath: String
) {
    ARTICLE("article", "articles"),
    ANSWER("answer", "answers");

    companion object {
        fun from(value: String?): ContentType {
            return entries.find {
                it.type.equals(value, ignoreCase = true) ||
                        it.apiPath.equals(value, ignoreCase = true)
            } ?: ANSWER
        }
    }
}