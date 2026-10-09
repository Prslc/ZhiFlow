package com.prslc.zhiflow.data.mapper

/** What a stats line reports: the votes it counts, and the comments. */
internal data class VoteStats(
    val voteCount: Int,
    val commentCount: Int,
)

/**
 * Matches strings like "7564 赞同 · 103 评论", "1.2 万赞同 · 252 评论".
 * Group 1: vote num, Group 2: 万 suffix, Group 3: comment num, Group 4: 万 suffix.
 */
private val VOTE_STAT_REGEX = Regex("([\\d.]+)\\s*(万?)\\s*赞同\\s*[·•.]\\s*([\\d.]+)\\s*(万?)\\s*评论")

/**
 * Reads the counts out of a stats line, or null when the text is not one.
 *
 * The server writes its counts as a sentence -- "2753 赞同 · 127 评论", with the type badge it
 * carries after them -- and the numbers are all this app draws from it: the words around them are
 * the reader's own, taken from their strings.
 *
 * @param text The line to read, which may carry more than the counts.
 */
internal fun voteStatsOf(text: String?): VoteStats? {
    val match = text?.let(VOTE_STAT_REGEX::find) ?: return null

    return VoteStats(
        voteCount = parseCount(match.groupValues[1], match.groupValues[2]),
        commentCount = parseCount(match.groupValues[3], match.groupValues[4]),
    )
}

/**
 * What one captured number and its 万 suffix add up to, for the lines the server abbreviates.
 *
 * @param value The digits, as they were written.
 * @param wanSuffix "万" when the count is written in tens of thousands, empty otherwise.
 */
internal fun parseCount(value: String, wanSuffix: String): Int {
    val base = value.toDoubleOrNull() ?: 0.0
    return if (wanSuffix == "万") (base * 10_000).toInt() else base.toInt()
}
