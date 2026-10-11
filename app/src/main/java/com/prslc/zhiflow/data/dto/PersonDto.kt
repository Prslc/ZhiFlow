package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable

/** One person to write to, as the picker lists them. */
@Immutable
data class PersonDto(
    val id: String,
    val urlToken: String,
    val name: String,
    val headline: String,
    val avatarUrl: String?,
)
