package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString

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
    data class Content(
        val id: String,
        val type: String,
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

/**
 * One narrower search the results page offers for the phrase it is showing.
 *
 * The server answers with an ending and the phrase it completes — `单词` for a search of `test` —
 * and the two are not interchangeable: the ending is what reads as a chip, and the whole phrase is
 * what a search of it has to use, since the ending on its own looks for something else entirely.
 */
@Immutable
data class SuggestedQueryDto(
    val suffix: String,
    val query: String,
)
