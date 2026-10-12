package com.prslc.zhiflow.data.model.user
import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.model.Paging
import com.prslc.zhiflow.data.model.content.AnswerAuthor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class CollectionResponse(
    val data: List<ZhihuCollection>,
    val paging: Paging? = null,
    val totals: Int = 0,
)

@Immutable
@Serializable
data class ZhihuCollection(
    val id: Long,
    val title: String,
    val description: String? = "",
    @SerialName("is_favorited") val isFavorited: Boolean,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("item_count") val itemCount: Int,
    @SerialName("updated_time") val updatedTime: Long,
    @SerialName("is_default") val isDefault: Boolean = false,
    val creator: AnswerAuthor? = null,
)
