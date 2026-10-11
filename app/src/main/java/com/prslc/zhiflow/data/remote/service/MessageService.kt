package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.ApiClient
import com.prslc.zhiflow.core.network.BASE_URL
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.data.model.notification.ZhihuMessageUser
import com.prslc.zhiflow.data.model.notification.ZhihuMessagesResponse
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

/**
 * Service handling private conversations.
 *
 * Reading one is plain text, and this app only reads: a conversation is opened, never answered.
 */
class MessageService(private val api: ApiClient) {

    /**
     * Retrieves the newest page of one conversation.
     *
     * @param peerId The person the conversation is with, by their user hash. The parameter the
     *   server wants is named for the sender of the messages, but it is the other end of the
     *   conversation that it takes.
     * @return A [Result] holding the messages, newest first, or the failure that kept them away.
     */
    suspend fun conversation(peerId: String): Result<ZhihuMessagesResponse> = api.safeApiCall {
        val url = "${BASE_URL}/messages".toHttpUrl().newBuilder()
            .addQueryParameter("sender_id", peerId)
            .addQueryParameter("limit", PAGE_SIZE.toString())
            .build()

        Request.Builder()
            .url(url)
            .get()
            .build()
    }

    /**
     * Retrieves who a conversation is with, which is what its page is headed by.
     *
     * @param peerId The same hash [conversation] takes.
     * @return A [Result] holding that person, or the failure that kept them away.
     */
    suspend fun peer(peerId: String): Result<ZhihuMessageUser> = api.safeApiCall {
        Request.Builder()
            .url("${BASE_URL}/messages/user/$peerId")
            .get()
            .build()
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
