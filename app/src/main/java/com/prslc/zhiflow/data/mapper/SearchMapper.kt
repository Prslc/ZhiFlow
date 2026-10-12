package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.HotQueryDto
import com.prslc.zhiflow.data.dto.ImageData
import com.prslc.zhiflow.data.dto.PersonDto
import com.prslc.zhiflow.data.dto.SearchItemDto
import com.prslc.zhiflow.data.dto.SuggestedQueryDto
import com.prslc.zhiflow.data.model.search.ZhihuPresetWordsResponse
import com.prslc.zhiflow.data.model.search.ZhihuSearchContent
import com.prslc.zhiflow.data.model.search.ZhihuSearchResponse
import com.prslc.zhiflow.data.model.search.ZhihuSuggestedQuery
import com.prslc.zhiflow.data.remote.parser.htmlToPlainText
import com.prslc.zhiflow.data.remote.parser.searchTextToAnnotatedString

/** What the `object` of a card says it holds, when the app knows how to draw it. */
private val CONTENT_TYPES = setOf("article", "answer")

/** The kind of card the picker reads: a person, rather than something to read. */
private const val PERSON_TYPE = "people"

/** The card kinds this app reads. The rest of the page is cards of other kinds, dropped. */
private const val RESULT_TYPE = "search_result"
private const val RELATED_TYPE = "relevant_query"

/**
 * Turns a page of results into the items this app draws.
 *
 * A card of a kind the app does not draw is dropped rather than guessed at: the page mixes
 * results with cards of half a dozen other kinds, decoding each of them into its own shape.
 * Nothing is silently lost that was going to be shown anyway.
 *
 * The related searches are lifted out of wherever the server put them and gathered at the end.
 * To it they are one card among many, and it scatters them through the results; a page meant to be
 * read top to bottom should not keep stopping for a list of other things to search for.
 *
 * @return The results in the order the server ranked them, then one row of related searches.
 */
internal fun ZhihuSearchResponse.toItems(): List<SearchItemDto> {
    val results = data.mapNotNull { card ->
        card.takeIf { it.type == RESULT_TYPE }
            ?.content
            ?.takeIf { it.type in CONTENT_TYPES && it.id.isNotBlank() }
            ?.toItem()
    }

    val related = data
        .filter { it.type == RELATED_TYPE }
        .flatMap { it.queryList }
        .map { it.query }
        .filter { it.isNotBlank() }
        .distinct()
        .takeIf { it.isNotEmpty() }
        ?.let(SearchItemDto::RelatedQueries)

    return results + listOfNotNull(related)
}

/**
 * @return The narrower search as a chip: the ending for the words on it, the whole phrase for the
 *   search behind it. An ending the server left out falls back to the phrase, and the other way
 *   around, so a chip is never empty and never searches for nothing.
 */
internal fun ZhihuSuggestedQuery.toDto() = SuggestedQueryDto(
    suffix = query.ifBlank { realQuery },
    query = realQuery.ifBlank { query },
)

/** @return The result as a row, with the words the query matched left marked in its title. */
private fun ZhihuSearchContent.toItem(): SearchItemDto.Content = SearchItemDto.Content(
    id = id,
    type = type,
    title = searchTextToAnnotatedString(title),
    excerpt = searchTextToAnnotatedString(excerpt),
    authorName = htmlToPlainText(author.name),
    authorAvatar = author.avatarUrl.takeIf { it.isNotBlank() },
    authorUrlToken = author.urlToken,
    images = thumbnailInfo.thumbnails.map { ImageData(it.url, it.width, it.height) }
        .filter { it.url.isNotBlank() },
    voteCount = voteupCount,
    commentCount = commentCount,
)

/**
 * The people a search for them found, which is what the picker lists.
 *
 * @return The people, in the order the server ranked them, with the markup read off their names.
 */
internal fun ZhihuSearchResponse.toPeople(): List<PersonDto> = data.mapNotNull { card ->
    card.takeIf { it.type == RESULT_TYPE }
        ?.content
        ?.takeIf { it.type == PERSON_TYPE && it.id.isNotBlank() }
        ?.toPerson()
}

private fun ZhihuSearchContent.toPerson(): PersonDto = PersonDto(
    id = id,
    urlToken = urlToken,
    name = htmlToPlainText(name),
    headline = htmlToPlainText(headline),
    avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
    isFollowing = isFollowing,
)

/**
 * Turns the words offered before anything is typed into the rows the search box shows.
 *
 * @return The phrases, each carrying whether the server marked it as a hot search.
 */
internal fun ZhihuPresetWordsResponse.toHotQueries(): List<HotQueryDto> = presetWords.words
    .map { word ->
        // The phrase to search for is `real_query`; `query` is what is drawn, and for a preset
        // word the two are the same string.
        HotQueryDto(query = word.realQuery.ifBlank { word.query }, isHot = word.label == "hot")
    }
    .filter { it.query.isNotBlank() }
