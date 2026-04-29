package com.prslc.zhiflow.parser

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.net.toUri
import com.prslc.zhiflow.ui.navigation.AnswerDetail
import com.prslc.zhiflow.ui.navigation.ArticleDetail
import com.prslc.zhiflow.ui.navigation.PeopleDetail
import com.prslc.zhiflow.ui.navigation.PinDetail
import com.prslc.zhiflow.ui.navigation.QuestionDetail

@Stable
sealed class LinkDestination {
    // Internal route object (e.g. PeopleDetail)
    data class Internal(val route: Any) : LinkDestination()
    // Fallback external URL
    @Immutable
    data class External(val url: String) : LinkDestination()
}

@Stable
object LinkParser {
    fun parse(url: String, contentType: String? = null): LinkDestination {
        val uri = url.toUri()

        // Handle Zhihu's redirect service
        val finalUrl = if (uri.host == "link.zhihu.com") {
            uri.getQueryParameter("target") ?: url
        } else {
            url
        }

        val finalUri = finalUrl.toUri()
        val host = finalUri.host ?: ""
        val path = finalUrl.substringBefore("?")

        // Security check: only allow internal navigation for zhihu.com
        if (!host.contains("zhihu.com")) {
            return LinkDestination.External(finalUrl)
        }

        val id = extractId(path)
        val type = contentType?.lowercase() ?: detectTypeFromPath(path)

        // Map to type-safe route
        val route = if (id != null && type != null) {
            mapToRoute(id, type)
        } else null

        return if (route != null) {
            LinkDestination.Internal(route)
        } else {
            LinkDestination.External(finalUrl)
        }
    }

    /**
     * Map type and ID to a specific Navigation route.
     */
    private fun mapToRoute(id: String, type: String): Any? = when (type) {
        "people" -> PeopleDetail(id)
        "answer" -> AnswerDetail(id)
        "article", "post" -> ArticleDetail(id)
        "question" -> QuestionDetail(id)
        "pin" -> PinDetail(id)
        else -> null
    }

    /**
     * Identify content type from URL path patterns.
     */
    private fun detectTypeFromPath(path: String): String? = when {
        path.contains("/people/") -> "people"
        path.contains("/answer/") -> "answer"
        path.contains("/p/") || path.contains("zhuanlan.zhihu.com") -> "article"
        path.contains("/question/") -> "question"
        path.contains("/pin/") -> "pin"
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
        return if (last.all { it.isDigit() }) last else null
    }
}