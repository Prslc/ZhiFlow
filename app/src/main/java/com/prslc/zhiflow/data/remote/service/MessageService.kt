package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.ApiClient
import com.prslc.zhiflow.core.network.BASE_URL
import com.prslc.zhiflow.core.network.formOf
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.core.network.sealedBody
import com.prslc.zhiflow.data.model.notification.ZhihuMessage
import com.prslc.zhiflow.data.model.notification.ZhihuMessageUser
import com.prslc.zhiflow.data.model.notification.ZhihuMessagesResponse
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Service handling private conversations.
 *
 * Reading one is plain text; what this app writes is sealed, which the body's own builder does.
 */
class MessageService(private val api: ApiClient) {

    /**
     * Retrieves a page of one conversation, newest first.
     *
     * @param peerId The person the conversation is with, by their user hash. The parameter the
     *   server wants is named for the sender of the messages, but it is the other end of the
     *   conversation that it takes.
     * @param nextUrl The page to ask for, from `paging.next` of a previous response. That url
     *   already carries the conversation and the cursor, so nothing is added to it; null asks for a
     *   first page, whose parameters are set here.
     * @return A [Result] holding the messages, newest first, or the failure that kept them away.
     */
    suspend fun conversation(
        peerId: String,
        nextUrl: String? = null,
    ): Result<ZhihuMessagesResponse> = api.safeApiCall {
        val url = nextUrl?.toHttpUrl()
            ?: "${BASE_URL}/messages".toHttpUrl().newBuilder()
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

    /**
     * Sends one message into a conversation.
     *
     * The body is the one this app seals, so it goes without a signature header; the server answers
     * with the message as it stored it, which is what the page then draws.
     *
     * @param peerId The person the conversation is with, by their user hash.
     * @param content The words to send, as the reader typed them.
     * @param sourceType How the conversation was opened, when the card that opened it said: the
     *   server is told the same thing the page was, and nothing is invented when it said nothing.
     * @return A [Result] holding the message the server took, or the failure that kept it back.
     */
    suspend fun send(
        peerId: String,
        content: String,
        sourceType: String?,
    ): Result<ZhihuMessage> = api.safeApiCall {
        val form = formOf(
            "receiver_id" to peerId,
            "content" to content,
            "content_type" to CONTENT_TYPE_TEXT,
            "source_type" to sourceType,
        )

        Request.Builder()
            .url("${BASE_URL}/messages")
            .sealedBody(form)
            .build()
    }

    /**
     * Deletes one message from this side of a conversation.
     *
     * The server keeps no tombstone for it: the message is gone from the list rather than marked,
     * which is why nothing comes back to draw.
     *
     * @param messageId The message's own id, as the conversation page received it.
     * @return A [Result] carrying the failure when the server refused.
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> = api.safeExecute {
        Request.Builder()
            .url("${BASE_URL}/messages/$messageId")
            .delete(ByteArray(0).toRequestBody())
            .build()
    }

    private companion object {
        const val PAGE_SIZE = 20

        /** Plain text, which is all this app sends. */
        const val CONTENT_TYPE_TEXT = "0"
    }
}
