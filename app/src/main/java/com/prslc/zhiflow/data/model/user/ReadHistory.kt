package com.prslc.zhiflow.data.model.user

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.model.Paging
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class ReadHistoryResponse(
    val paging: Paging = Paging(),
    val data: List<ReadHistoryCard> = emptyList(),
)

@Immutable
@Serializable
data class ReadHistoryCard(
    @SerialName("card_type") val cardType: String = "",
    val data: ReadHistoryCardData? = null,
)

@Immutable
@Serializable
data class ReadHistoryCardData(
    val header: ReadHistoryHeader? = null,
    val content: ReadHistoryContent? = null,
    val matrix: List<MatrixItem> = emptyList(),
    val action: ReadHistoryAction? = null,
    val extra: ReadHistoryExtra? = null,
)

@Immutable
@Serializable
data class ReadHistoryHeader(
    val icon: String? = null,
    val title: String? = null,
    val action: ReadHistoryAction? = null,
)

@Immutable
@Serializable
data class ReadHistoryContent(
    @SerialName("author_name") val authorName: String? = null,
    val summary: String? = null,
    @SerialName("cover_image") val coverImage: String? = null,
)

@Immutable
@Serializable
data class MatrixItem(
    val type: String = "",
    val data: MatrixData? = null,
)

@Immutable
@Serializable
data class MatrixData(
    val text: String = "",
)

@Immutable
@Serializable
data class ReadHistoryAction(
    val type: String? = null,
    val url: String? = null,
)

@Immutable
@Serializable
data class ReadHistoryExtra(
    @SerialName("content_token") val contentToken: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("read_time") val readTime: Long = 0,
    @SerialName("question_token") val questionToken: String? = null,
    @SerialName("share_info") val shareInfo: String? = null,
    @SerialName("mcn_source") val mcnSource: String? = null,
)
