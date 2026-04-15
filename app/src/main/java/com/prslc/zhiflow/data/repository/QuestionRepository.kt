package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.service.QuestionService
import com.prslc.zhiflow.data.model.QuestionDetail
import com.prslc.zhiflow.data.model.QuestionFeedResponse

class QuestionRepository(private val service: QuestionService) {

    /**
     * Fetch detailed information of a specific question
     *
     * @param id The unique identifier of the question
     * @return A [Result] containing [QuestionDetail] on success, or an exception on failure
     */
    suspend fun getQuestion(id: String) = runCatching {
        service.getQuestionDetail(id)
    }

    /**
     * Fetch the answer feed for a specific question (supports pagination)
     *
     * @param id The unique identifier of the question
     * @param nextUrl URL for the next page of answers; if null, fetches the first page
     * @return A [Result] containing [QuestionFeedResponse] on success, or an exception on failure
     */
    suspend fun getQuestionFeed(id: String, nextUrl: String? = null) = runCatching {
        service.getQuestionFeed(id, nextUrl)
    }
}