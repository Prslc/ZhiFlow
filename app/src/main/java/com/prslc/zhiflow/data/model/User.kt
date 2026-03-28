package com.prslc.zhiflow.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZhihuUser(
    @SerialName("name") val name: String? = null,
    @SerialName("headline") val headline: String? = null,
    @SerialName("description") val userDescription: String? = null,
    @SerialName("avatar_url") val avatar: String? = null,

    @SerialName("follower_count") val followerCount: Int = 0, // fans
    @SerialName("following_count") val followingCount: Int = 0, // following
    @SerialName("favorite_count") val favoriteCount: Int = 0    // favorite
)