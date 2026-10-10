package com.prslc.zhiflow.data.model.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuestToken(
    @SerialName("id") val id: String = "",
    @SerialName("uid") val uid: Long = 0L,
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("token_type") val tokenType: String = "",
    @SerialName("user_type") val userType: String = "",
    @SerialName("push_channel") val pushChannel: String = "",
    @SerialName("cookie") val cookie: SessionCookie = SessionCookie(),
    @SerialName("created_at") val createdAt: Long = 0L,
)
