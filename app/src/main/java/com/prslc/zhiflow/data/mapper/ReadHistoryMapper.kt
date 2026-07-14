package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.ReadHistoryDto
import com.prslc.zhiflow.data.model.user.ReadHistoryCardData

/**
 * Matches strings like "7564 赞同 · 103 评论", "1.2 万赞同 · 252 评论".
 * Group 1: vote num, Group 2: 万 suffix, Group 3: comment num, Group 4: 万 suffix.
 */
private val VOTE_STAT_REGEX = Regex("([\\d.]+)\\s*(万?)\\s*赞同\\s*[·•.]\\s*([\\d.]+)\\s*(万?)\\s*评论")

/**
 * Matches strings like "已读 14%", "已读 100%".
 */
private val READ_PROGRESS_REGEX = Regex("已读\\s*([\\d.]+)%")

/**
 * Matches strings like "1068 回答 · 1.1 万关注".
 */
private val QUESTION_STAT_REGEX = Regex("([\\d.]+)\\s*(万?)\\s*回答\\s*[·•.]\\s*([\\d.]+)\\s*(万?)\\s*关注")

/**
 * Matches strings like "2.4 万赞同 · 264 人关注".
 */
private val PROFILE_STAT_REGEX = Regex("([\\d.]+)\\s*(万?)\\s*赞同\\s*[·•.]\\s*([\\d.]+)\\s*(万?)\\s*人关注")

private fun parseCount(value: String, wanSuffix: String): Int {
    val base = value.toDoubleOrNull() ?: 0.0
    return if (wanSuffix == "万") (base * 10_000).toInt() else base.toInt()
}

internal fun ReadHistoryCardData.toDto(): ReadHistoryDto {
    val contentType = extra?.contentType.orEmpty()
    val statText = matrix.getOrNull(0)?.data?.text.orEmpty()

    return when (contentType) {
        "question" -> {
            val match = QUESTION_STAT_REGEX.find(statText)
            ReadHistoryDto(
                questionTitle = header?.title.orEmpty(),
                questionToken = extra?.questionToken,
                contentTypeIcon = header?.icon,
                contentType = contentType,
                authorName = null,
                summary = null,
                coverImage = null,
                voteCount = 0,
                commentCount = 0,
                readProgress = 0,
                answerCount = match?.let { parseCount(it.groupValues[1], it.groupValues[2]) } ?: 0,
                followerCount = match?.let { parseCount(it.groupValues[3], it.groupValues[4]) } ?: 0,
                contentToken = extra?.contentToken,
                readTime = extra?.readTime ?: 0,
            )
        }
        "profile" -> {
            val match = PROFILE_STAT_REGEX.find(statText)
            ReadHistoryDto(
                questionTitle = header?.title.orEmpty(),
                questionToken = extra?.questionToken,
                contentTypeIcon = header?.icon,
                contentType = contentType,
                authorName = null,
                summary = null,
                coverImage = null,
                voteCount = match?.let { parseCount(it.groupValues[1], it.groupValues[2]) } ?: 0,
                commentCount = 0,
                readProgress = 0,
                answerCount = 0,
                followerCount = match?.let { parseCount(it.groupValues[3], it.groupValues[4]) } ?: 0,
                contentToken = extra?.contentToken,
                readTime = extra?.readTime ?: 0,
            )
        }
        else -> {
            val voteMatch = VOTE_STAT_REGEX.find(statText)
            val progressText = matrix.getOrNull(1)?.data?.text.orEmpty()
            val progressMatch = READ_PROGRESS_REGEX.find(progressText)
            ReadHistoryDto(
                questionTitle = header?.title.orEmpty(),
                questionToken = extra?.questionToken,
                contentTypeIcon = header?.icon,
                contentType = contentType,
                authorName = content?.authorName,
                summary = content?.summary,
                coverImage = content?.coverImage?.takeIf { it.isNotBlank() },
                voteCount = voteMatch?.let { parseCount(it.groupValues[1], it.groupValues[2]) } ?: 0,
                commentCount = voteMatch?.let { parseCount(it.groupValues[3], it.groupValues[4]) } ?: 0,
                readProgress = progressMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                answerCount = 0,
                followerCount = 0,
                contentToken = extra?.contentToken,
                readTime = extra?.readTime ?: 0,
            )
        }
    }
}
