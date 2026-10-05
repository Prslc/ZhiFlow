package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.core.network.BASE_URL
import com.prslc.zhiflow.data.dto.FeedbackAction
import com.prslc.zhiflow.data.model.feedback.NegativeFeedbackItem
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

private val apiHost = BASE_URL.toHttpUrl().host

/** Returns `null` for a row the client cannot act on, so an unknown module is dropped. */
internal fun NegativeFeedbackItem.toDto(): FeedbackAction? {
    val button = rawButton
    val label = button.text.panelText
    if (label.isBlank()) return null

    val moduleId = button.action.module?.moduleId.orEmpty()
    val iconUrl = button.icon?.imageUrl
    val nightIconUrl = button.icon?.nightImageUrl
    val hasChevron = button.rightIcon != null
    val maxLines = button.text.maxLine.coerceAtLeast(1)

    button.action.backendUrl?.takeIf { it.isApiUrl() }?.let { url ->
        return FeedbackAction.Request(
            moduleId = moduleId,
            label = label,
            iconUrl = iconUrl,
            nightIconUrl = nightIconUrl,
            hasChevron = hasChevron,
            maxLines = maxLines,
            url = url,
            method = button.action.method,
            toastText = button.text.toastText,
        )
    }

    button.action.intentUrl?.let { url ->
        return FeedbackAction.OpenUrl(
            moduleId = moduleId,
            label = label,
            iconUrl = iconUrl,
            nightIconUrl = nightIconUrl,
            hasChevron = hasChevron,
            maxLines = maxLines,
            url = url,
        )
    }

    return null
}

/**
 * The OkHttp client stamps auth headers onto every outgoing request, so a URL the
 * backend merely names must not be followed off-host.
 */
private fun String.isApiUrl(): Boolean = toHttpUrlOrNull()?.host == apiHost
