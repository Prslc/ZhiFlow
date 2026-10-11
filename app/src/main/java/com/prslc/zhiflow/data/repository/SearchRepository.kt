package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.core.exception.mapOffMain
import com.prslc.zhiflow.data.dto.HotQueryDto
import com.prslc.zhiflow.data.dto.SearchItemDto
import com.prslc.zhiflow.data.mapper.toHotQueries
import com.prslc.zhiflow.data.mapper.toItems
import com.prslc.zhiflow.data.remote.service.SearchService

data class SearchResult(
    val items: List<SearchItemDto>,
    val nextPageUrl: String?,
    val isEnd: Boolean,
)

class SearchRepository(private val service: SearchService) {

    /**
     * Runs a search, or asks for the next page of one.
     *
     * @param query What to search for.
     * @param tab Which result tab to search in. `general` is the one a search opens on.
     * @param nextUrl The page to request, from a previous response's cursor. Null asks for a
     *   first page, which is also where [query] and [tab] are read from.
     * @return A [Result] holding the items, the cursor for the page after them when there is one,
     *   and whether the server has said the results end here.
     */
    suspend fun search(
        query: String,
        tab: String = "general",
        nextUrl: String? = null,
    ): Result<SearchResult> = service.search(query, tab, nextUrl)
        .mapOffMain { response ->
            SearchResult(
                items = response.toItems(),
                nextPageUrl = response.paging.next.takeIf { it.isNotBlank() },
                isEnd = response.paging.isEnd,
            )
        }

    /**
     * Asks for what the reader might mean, given what they have typed so far.
     *
     * @param query The text so far.
     * @return A [Result] holding the phrases, each ready to be searched for as it stands.
     */
    suspend fun suggest(query: String): Result<List<String>> = service.suggest(query)
        .mapOffMain { response ->
            response.suggest.map { it.query }.filter { it.isNotBlank() }
        }

    /**
     * Asks for the narrower phrases that follow a search already run.
     *
     * @param query The phrase that was searched for.
     * @return A [Result] holding phrases that are whole searches in themselves. The ending alone
     *   is never handed back, since searching for it would look for something else entirely.
     */
    suspend fun suggestedQueries(query: String): Result<List<String>> =
        service.suggestedQueries(query)
            .mapOffMain { response ->
                response.queryList
                    .map { it.realQuery.ifBlank { it.query } }
                    .filter { it.isNotBlank() }
            }

    /**
     * Asks for the words to offer before anything has been typed.
     *
     * @return A [Result] holding the phrases, each carrying whether the server marked it hot.
     */
    suspend fun hotQueries(): Result<List<HotQueryDto>> = service.presetWords()
        .mapOffMain { it.toHotQueries() }
}
