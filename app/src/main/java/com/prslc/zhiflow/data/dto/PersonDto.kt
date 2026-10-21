package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable

/**
 * One person as a list draws them: who to write to, and who the account is following.
 *
 * @property profileToken The identifier a profile page is addressed by: the url token a link names
 *   the account by, or its hash when it has none. Resolved by the mapper, so a tap never chooses.
 */
@Immutable
data class PersonDto(
    val id: String,
    val profileToken: String,
    val name: String,
    val headline: String,
    val avatarUrl: String?,
    val isFollowing: Boolean,
)
