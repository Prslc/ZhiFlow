package com.prslc.zhiflow.parser

import androidx.core.net.toUri

sealed class LinkDestination {
    data class Internal(val id: String, val type: String) : LinkDestination()
    data class External(val url: String) : LinkDestination()
}

object LinkParser {
    fun parse(url: String, contentType: String? = null): LinkDestination {
        val uri = url.toUri()

        // Redirect
        val finalUrl = if (uri.host == "link.zhihu.com") {
            uri.getQueryParameter("target") ?: url
        } else {
            url
        }

        val finalUri = finalUrl.toUri()
        val host = finalUri.host ?: ""
        val path = finalUrl.substringBefore("?")

        /** Dirty data alert: metadata says ANSWER, but it's an external redirect.
         *  We must verify if it's actually a Zhihu link before internal navigation.
         */
        if (!host.contains("zhihu.com")) {
            return LinkDestination.External(finalUrl)
        }

        val metadataType = when (contentType?.uppercase()) {
            "ANSWER" -> "answer"
            "ARTICLE", "POST" -> "article"
            "QUESTION" -> "question"
            "PIN" -> "pin"
            else -> null
        }

        val id = extractId(path)

        return if (metadataType != null && id != null) {
            LinkDestination.Internal(id, metadataType)
        } else {
            parseFromPath(finalUrl, path)
        }
    }

    private fun parseFromPath(originalUrl: String, path: String): LinkDestination {
        val id = extractId(path) ?: return LinkDestination.External(originalUrl)

        val type = when {
            path.contains("/answer/") -> "answer"
            path.contains("/p/") || path.contains("zhuanlan.zhihu.com") -> "article"
            path.contains("/question/") -> "question"
            path.contains("/pin/") -> "pin"
            else -> null
        }

        return if (type != null) {
            LinkDestination.Internal(id, type)
        } else {
            LinkDestination.External(originalUrl)
        }
    }

    private fun extractId(path: String): String? {
        val lastSegment = path.trimEnd('/').split("/").lastOrNull().orEmpty()
        return if (lastSegment.isNotEmpty() && lastSegment.all { it.isDigit() }) {
            lastSegment
        } else null
    }
}