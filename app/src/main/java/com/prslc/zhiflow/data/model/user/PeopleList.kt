package com.prslc.zhiflow.data.model.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One page of an account's people, which is a flat list rather than a page of cards.
 *
 * The people an account follows and the people following it answer with the same shape; the
 * followees list adds a `contacts` object on top, which nothing here reads.
 */
@Serializable
data class ZhihuPeoplePage(
    val data: List<ZhihuPerson> = emptyList(),
    val paging: ZhihuPeoplePaging = ZhihuPeoplePaging(),
)

/** One of them, read for the name, the line under it and the picture beside it. */
@Serializable
data class ZhihuPerson(
    val id: String = "",
    @SerialName("url_token") val urlToken: String = "",
    val name: String = "",
    val headline: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
)

@Serializable
data class ZhihuPeoplePaging(
    @SerialName("is_end") val isEnd: Boolean = false,
    val next: String = "",
)
