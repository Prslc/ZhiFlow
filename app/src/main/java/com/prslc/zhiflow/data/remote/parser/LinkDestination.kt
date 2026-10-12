package com.prslc.zhiflow.data.remote.parser

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.net.toUri

@Stable
sealed class LinkDestination {
    // What the address names — the kind of thing, and its id — for the navigator to open.
    data class Internal(val type: String, val id: String) : LinkDestination()

    // Fallback external URL
    @Immutable
    data class External(val url: String) : LinkDestination()
}

@Immutable
object LinkParser {
    private const val REDIRECT_HOST = "link.zhihu.com"

    /** The app's own scheme, and the authority a column is carried under on it. */
    private const val APP_SCHEME = "zhihu"
    private const val COLUMN_AUTHORITY = "column"

    /**
     * Resolves [url]: outbound links arrive wrapped in a redirect whose parameter holds the target,
     * and Zhihu's own URLs name something the app opens while everything else stays
     * [LinkDestination.External].
     */
    fun parse(url: String): LinkDestination {
        val finalUrl = unwrapRedirect(url)

        columnTokenOf(finalUrl)?.let { return LinkDestination.Internal("column", it) }

        val finalUri = finalUrl.toUri()
        val host = finalUri.host ?: ""
        val path = finalUrl.substringBefore("?")

        // Security check: only allow internal navigation for zhihu.com
        if (!host.contains("zhihu.com")) {
            return LinkDestination.External(finalUrl)
        }

        val id = extractId(path)
        val type = detectTypeFromPath(path)

        return if (id != null && type != null) {
            LinkDestination.Internal(type, id)
        } else {
            LinkDestination.External(finalUrl)
        }
    }

    /**
     * The column a `zhihu://column/{token}` link names, or null when [url] is not one.
     *
     * A column's own links are written in the app's scheme rather than as an address, which is what
     * the card at the end of a body published in one carries.
     */
    private fun columnTokenOf(url: String): String? {
        val uri = url.toUri()
        return if (uri.scheme == APP_SCHEME && uri.authority == COLUMN_AUTHORITY) {
            uri.lastPathSegment?.takeIf { it.isNotBlank() }
        } else {
            null
        }
    }

    /**
     * The address a redirect link points at, or [url] itself when it is not one: outbound links
     * arrive wrapped in `link.zhihu.com/?target=`.
     */
    internal fun unwrapRedirect(url: String): String {
        val uri = url.toUri()
        return if (uri.host == REDIRECT_HOST) {
            uri.getQueryParameter("target") ?: url
        } else {
            url
        }
    }

    /** Whether [url] is a link out still wrapped in `link.zhihu.com/?target=`, not an address. */
    internal fun isRedirect(url: String): Boolean = url.toUri().host == REDIRECT_HOST

    /**
     * Identify content type from URL path patterns.
     */
    private fun detectTypeFromPath(path: String): String? = when {
        path.contains("/people/") -> "people"
        path.contains("/answer/") -> "answer"
        path.contains("/p/") || path.contains("zhuanlan.zhihu.com") -> "article"
        path.contains("/question/") -> "question"
        path.contains("/pin/") || path.contains("/pins/") -> "pin"
        path.contains("/column/") -> "column"
        else -> null
    }

    /**
     * Extracts ID. 'people' allows alphanumeric; others must be numeric.
     */
    private fun extractId(path: String): String? {
        val segments = path.substringBefore("?").split("/").filter { it.isNotEmpty() }
        if (segments.isEmpty()) return null

        val peopleIndex = segments.indexOf("people")
        if (peopleIndex != -1) {
            return segments.getOrNull(peopleIndex + 1)
        }

        val last = segments.last()
        // A column is named by a token rather than by a number (`c_1867989249418211328`).
        if (segments.contains("column")) return last.takeIf { it.isNotBlank() }

        return if (last.all { it.isDigit() }) last else null
    }
}
