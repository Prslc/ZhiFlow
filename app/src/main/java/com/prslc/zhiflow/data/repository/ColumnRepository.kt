package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.core.exception.mapOffMain
import com.prslc.zhiflow.data.dto.ColumnDto
import com.prslc.zhiflow.data.dto.ColumnItemDto
import com.prslc.zhiflow.data.mapper.toDto
import com.prslc.zhiflow.data.remote.service.ColumnService

/** A page of a column's list, and whether it was the last of them. */
data class ColumnItemsResult(
    val items: List<ColumnItemDto>,
    val isEnd: Boolean,
)

class ColumnRepository(
    private val service: ColumnService,
) {

    /**
     * Fetches a column's own page.
     *
     * @param urlToken The column's token.
     * @return A [Result] holding the column, or the failure that kept it away.
     */
    suspend fun detail(urlToken: String): Result<ColumnDto> =
        service.detail(urlToken).mapOffMain { it.toDto() }

    /**
     * Fetches a page of a column's list.
     *
     * @param urlToken The column's token.
     * @param offset How many entries to skip, which is the pages already read.
     * @return A [Result] holding the page — the end of the column is one with no entries in it —
     *   or the failure that kept it away.
     */
    suspend fun items(urlToken: String, offset: Int): Result<ColumnItemsResult> =
        service.items(urlToken, offset).mapOffMain { body ->
            ColumnItemsResult(
                items = body.data.map { it.toDto() },
                // A page that holds nothing ends the list whatever the server's own flag says: the
                // list is advanced by offset, so one that answered nothing would be asked for
                // again and again.
                isEnd = body.paging.isEnd || body.data.isEmpty(),
            )
        }

    /**
     * Follows a column.
     *
     * @param urlToken The column's token.
     * @return A [Result] indicating success or failure.
     */
    suspend fun follow(urlToken: String): Result<Unit> = service.follow(urlToken)

    /**
     * Unfollows a column.
     *
     * @param urlToken The column's token.
     * @return A [Result] indicating success or failure.
     */
    suspend fun unfollow(urlToken: String): Result<Unit> = service.unfollow(urlToken)
}
