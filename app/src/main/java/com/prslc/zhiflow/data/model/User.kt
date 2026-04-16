package com.prslc.zhiflow.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZhihuUser(
    @SerialName("id") val id: String = "",
    @SerialName("url_token") val urlToken: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("headline") val headline: String? = null,
    @SerialName("description") val userDescription: String? = null,
    @SerialName("avatar_url") val avatar: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null, // bg

    // Statistical data
    @SerialName("follower_count") val followerCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("voteup_count") val voteupCount: Int = 0,
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("pins_count") val pinsCount: Int = 0,
    @SerialName("answer_count") val answerCount: Int = 0
)