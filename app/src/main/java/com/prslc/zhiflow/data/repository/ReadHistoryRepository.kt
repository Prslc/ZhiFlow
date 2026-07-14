package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.dto.ReadHistoryDto
import com.prslc.zhiflow.data.mapper.toDto
import com.prslc.zhiflow.data.remote.service.ReadHistoryService

data class ReadHistoryResult(
    val items: List<ReadHistoryDto>,
    val nextPageUrl: String?,
    val isEnd: Boolean,
    val totals: Int,
)

class ReadHistoryRepository(private val service: ReadHistoryService) {

    /**
     * Fetches paginated read history, mapping raw API cards to display-ready DTOs
     * with locale-independent numeric fields extracted from Chinese stat strings.
     */
    suspend fun getReadHistory(offset: Int = 0, limit: Int = 10): Result<ReadHistoryResult> =
        service.getReadHistory(offset, limit).map { response ->
            ReadHistoryResult(
                items = response.data.mapNotNull { it.data?.toDto() },
                nextPageUrl = response.paging.next,
                isEnd = response.paging.isEnd,
                totals = response.paging.totals,
            )
        }
}
