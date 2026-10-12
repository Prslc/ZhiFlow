package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable

/** One person as a list draws them: who to write to, and who the account is following. */
@Immutable
data class PersonDto(
    val id: String,
    val urlToken: String,
    val name: String,
    val headline: String,
    val avatarUrl: String?,
    val isFollowing: Boolean,
)
