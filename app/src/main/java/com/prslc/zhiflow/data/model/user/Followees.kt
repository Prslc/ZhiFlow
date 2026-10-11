package com.prslc.zhiflow.data.model.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A page of the people an account follows, which is a flat list rather than a page of cards. */
@Serializable
data class ZhihuFolloweesResponse(
    val data: List<ZhihuFollowee> = emptyList(),
    val paging: ZhihuFolloweePaging = ZhihuFolloweePaging(),
)

/** One of them, read for the name, the line under it and the picture beside it. */
@Serializable
data class ZhihuFollowee(
    val id: String = "",
    @SerialName("url_token") val urlToken: String = "",
    val name: String = "",
    val headline: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
)

@Serializable
data class ZhihuFolloweePaging(
    @SerialName("is_end") val isEnd: Boolean = false,
    val next: String = "",
)
