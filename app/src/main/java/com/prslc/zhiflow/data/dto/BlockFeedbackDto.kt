package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable

@Immutable
data class BlockTag(
    val id: Long,
    val label: String,
    val isSelected: Boolean = false,
)

/** Everything the block page renders: the topics it offers and the keywords it holds. */
@Immutable
data class BlockOptions(
    val tags: List<BlockTag> = emptyList(),
    val keywords: List<String> = emptyList(),
    val keywordMinLength: Int = 0,
    val keywordMaxLength: Int = 0,
    val keywordMaxCount: Int = 0,
)

enum class KeywordIssue {
    TOO_SHORT,
    TOO_LONG,
    MAX_COUNT,
    DUPLICATE,
}
