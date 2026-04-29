package com.prslc.zhiflow.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class ReadHistoryRequest(
    @SerialName("content_token") val contentToken: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("read_progress") val readProgress: Int,
    @SerialName("listen_progress") val listenProgress: Int = 0,
    @SerialName("read_time") val readTime: Long = System.currentTimeMillis() / 1000,
    @SerialName("custom_content_data") val customContentData: String? = null
)