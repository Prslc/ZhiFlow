package com.prslc.zhiflow.core.network

import androidx.compose.runtime.Immutable

/**
 * One captured API call. Bodies are truncated and headers are never recorded,
 * so credentials cannot leak into the log screen.
 */
@Immutable
data class HttpLogEntry(
    val id: Long,
    val at: Long,
    val method: String,
    val url: String,
    val status: Int?,
    val durationMs: Long,
    val requestBody: String?,
    val responseBody: String?,
    val error: String?,
    val truncated: Boolean,
)
