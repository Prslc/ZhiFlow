package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.ApiClient
import com.prslc.zhiflow.core.network.WEB_URL
import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.data.model.column.ZhihuColumn
import com.prslc.zhiflow.data.model.column.ZhihuColumnItems
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * How many articles one page of a column's list asks for. The server defaults to the same, and
 * says the page size back in its cursor.
 */
private const val PAGE_SIZE = "10"

/**
 * Service for a column and the articles it holds.
 *
 * The three endpoints behind this are spread over two hosts and two prefixes, and the follow action
 * is a `PUT` where the same idea on a person is a `POST` — none of which follows from the others.
 */
class ColumnService(private val api: ApiClient) {

    /**
     * Fetches a column's own page.
     *
     * @param urlToken The column's token, as its routes name it (`c_1867989249418211328`).
     * @return A [Result] holding the column, or the failure that kept it away.
     */
    suspend fun detail(urlToken: String): Result<ZhihuColumn> = api.safeApiCall {
        Request.Builder()
            .apiUrl("/columns/v2/$urlToken")
            .get()
            .build()
    }

    /**
     * Fetches a page of a column's articles, newest first.
     *
     * The site host serves these, not the API host, and its paging is an explicit `limit`/`offset`
     * the caller advances rather than a cursor followed — the cursor it answers with names this
     * host over plain http.
     *
     * @param urlToken The column's token.
     * @param offset How many articles to skip, which is the pages already read.
     * @return A [Result] holding the page, or the failure that kept it away.
     */
    suspend fun items(urlToken: String, offset: Int): Result<ZhihuColumnItems> = api.safeApiCall {
        val url = "${WEB_URL}/api/v4/columns/$urlToken/items".toHttpUrl().newBuilder()
            .addQueryParameter("limit", PAGE_SIZE)
            .addQueryParameter("offset", offset.toString())
            .build()

        Request.Builder()
            .url(url)
            .get()
            .build()
    }

    /**
     * Follows a column.
     *
     * The follow direction was never captured; it is the mirror of [unfollow], which was. A server
     * that answers this with a rejection would say so on the column page, as any other failure.
     *
     * @param urlToken The column's token.
     * @return A [Result] indicating success or failure.
     */
    suspend fun follow(urlToken: String): Result<Unit> = api.safeExecute {
        Request.Builder()
            .apiUrl("/columns/$urlToken/follow")
            .put(ByteArray(0).toRequestBody())
            .build()
    }

    /**
     * Unfollows a column.
     *
     * @param urlToken The column's token.
     * @return A [Result] indicating success or failure.
     */
    suspend fun unfollow(urlToken: String): Result<Unit> = api.safeExecute {
        Request.Builder()
            .apiUrl("/columns/$urlToken/unfollow")
            .put(ByteArray(0).toRequestBody())
            .build()
    }
}
