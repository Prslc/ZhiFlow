package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.PersonDto
import com.prslc.zhiflow.data.model.user.ZhihuPerson

/** @return The person as a list of people draws them. */
internal fun ZhihuPerson.toPerson(): PersonDto = PersonDto(
    id = id,
    profileToken = urlToken.ifBlank { id },
    name = name,
    headline = headline,
    avatarUrl = avatarUrl.takeIf { it.isNotBlank() },
    isFollowing = isFollowing,
)
