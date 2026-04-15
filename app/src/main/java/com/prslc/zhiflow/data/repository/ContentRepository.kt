package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.service.ContentService

class ContentRepository(private val service: ContentService) {

    /**
     * Fetch answer detail by ID
     *
     * @param id The unique identifier of the answer
     * @return A [Result] containing [ZhihuAnswer] on success
     */
    suspend fun getAnswer(id: String) = runCatching { service.getAnswerDetail(id)!! }

    /**
     * Fetch article detail by ID
     *
     * @param id The unique identifier of the article
     * @return A [Result] containing [ZhihuArticle] on success
     */
    suspend fun getArticle(id: String) = runCatching { service.getArticleDetail(id)!! }

    /**
     * Fetch pin (thought) detail by ID
     *
     * @param id The unique identifier of the pin
     * @return A [Result] containing [ZhihuPin] on success
     */
    suspend fun getPin(id: String) = runCatching { service.getPinDetail(id)!! }
}