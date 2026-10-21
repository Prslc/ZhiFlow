package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable

/**
 * The account that asked a question, as the question page draws them: enough to tell who is
 * asking, and no more.
 *
 * @property profileToken The identifier a profile page is addressed by: the url token a link names
 *   the account by, or its hash when it has none. Resolved here so a tap never has to choose.
 */
@Immutable
data class QuestionAuthorDto(
    val profileToken: String,
    val name: String,
    val headline: String,
    val avatarUrl: String?,
)
