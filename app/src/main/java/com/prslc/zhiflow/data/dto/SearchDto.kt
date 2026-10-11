package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import com.prslc.zhiflow.ui.component.common.ImageData

/**
 * One entry in a page of results.
 *
 * A page is not a list of one thing: alongside the results proper it carries cards of other
 * kinds, and only the ones this app draws become an item.
 */
@Immutable
sealed interface SearchItemDto {

    /** A result the reader can open. */
    @Immutable
    data class Article(
        val id: String,
        val title: AnnotatedString,
        val excerpt: AnnotatedString,
        val authorName: String,
        val authorAvatar: String?,
        val authorUrlToken: String,
        val images: List<ImageData>,
        val voteCount: Int,
        val commentCount: Int,
    ) : SearchItemDto

    /** A row of narrower phrases to search for instead. */
    @Immutable
    data class RelatedQueries(
        val queries: List<String>,
    ) : SearchItemDto
}

/** One phrase the search box offers before anything has been typed. */
@Immutable
data class HotQueryDto(
    val query: String,
    val isHot: Boolean,
)
