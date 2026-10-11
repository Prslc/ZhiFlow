package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.PersonDto
import com.prslc.zhiflow.data.model.user.ZhihuFollowee

/** @return The person as the picker lists them. */
internal fun ZhihuFollowee.toPerson(): PersonDto = PersonDto(
    id = id,
    urlToken = urlToken,
    name = name,
    headline = headline,
    avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
)
