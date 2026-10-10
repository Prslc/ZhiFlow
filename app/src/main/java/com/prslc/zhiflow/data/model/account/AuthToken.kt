package com.prslc.zhiflow.data.model.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthToken(
    @SerialName("uid") val uid: String = "",
    @SerialName("user_id") val userId: Long = 0L,
    @SerialName("token_type") val tokenType: String = "",
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: Long = 0L,
    @SerialName("cookie") val cookie: SessionCookie = SessionCookie(),
)

@Serializable
data class SessionCookie(
    @SerialName("q_c0") val qCookie: String = "",
    @SerialName("z_c0") val zCookie: String = "",
)
