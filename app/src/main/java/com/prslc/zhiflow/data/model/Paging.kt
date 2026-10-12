package com.prslc.zhiflow.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The envelope a paged response arrives in.
 *
 * It sits at the root of the models rather than inside a domain: a page of answers, of comments and
 * of collections all come wrapped in this one, and none of them owns it. An endpoint that sends
 * fewer fields than these leaves them at their defaults.
 */
@Immutable
@Serializable
data class Paging(
    @SerialName("is_end") val isEnd: Boolean = false,
    @SerialName("is_start") val isStart: Boolean = false,
    val next: String? = null,
    val previous: String? = null,
    val totals: Int = 0,
)
