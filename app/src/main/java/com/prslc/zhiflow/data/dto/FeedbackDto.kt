package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable

/**
 * One selectable row of the negative feedback panel. Which variant a row maps to is
 * decided by what the backend sent, not by its `moduleId`.
 */
@Immutable
sealed interface FeedbackAction {
    val moduleId: String
    val label: String
    val iconUrl: String?
    val nightIconUrl: String?
    val hasChevron: Boolean
    val maxLines: Int

    @Immutable
    data class Request(
        override val moduleId: String,
        override val label: String,
        override val iconUrl: String?,
        override val nightIconUrl: String?,
        override val hasChevron: Boolean,
        override val maxLines: Int,
        /** Supplied by the backend and already checked to be on the API host. */
        val url: String,
        val method: String,
        val toastText: String?,
    ) : FeedbackAction

    @Immutable
    data class OpenUrl(
        override val moduleId: String,
        override val label: String,
        override val iconUrl: String?,
        override val nightIconUrl: String?,
        override val hasChevron: Boolean,
        override val maxLines: Int,
        val url: String,
    ) : FeedbackAction
}
