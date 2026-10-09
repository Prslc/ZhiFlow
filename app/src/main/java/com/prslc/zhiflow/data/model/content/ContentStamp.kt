package com.prslc.zhiflow.data.model.content

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * What a stamp on a piece of content says: when it was made, or how long ago that was.
 *
 * The server words these as text, and which of the two it is depends on how old the content is --
 * a date for anything settled, an age for something fresh. Both shapes are read here; a stamp that
 * is neither is left null, for the caller to draw as it came.
 */
sealed interface ContentStamp {
    /** A date and time, as in `2025-11-09 02:17`. */
    data class At(val value: LocalDateTime) : ContentStamp

    /** An age, as in `23 小时前`. */
    data class Ago(val count: Int, val unit: ContentAgeUnit) : ContentStamp

    /** Published, or edited, a moment ago. */
    data object JustNow : ContentStamp
}

/** The unit an age is worded with. */
enum class ContentAgeUnit { Second, Minute, Hour, Day, Week, Month, Year }

/**
 * The stamp the server writes for a date.
 *
 * One digit is asked of each field rather than two: the stamps seen so far are zero-padded, and a
 * stamp that is not would only be one this walked past.
 */
private val DATE_STAMP = DateTimeFormatter.ofPattern("uuuu-M-d H:m")

/** The stamp it writes for an age, e.g. `23 小时前`. */
private val AGE_STAMP = Regex("""(\d+)\s*(秒|分钟|小时|天|周|个月|月|年)前""")

/** What it writes instead of an age, for something published a moment ago. */
private const val JUST_NOW = "刚刚"

/** What it puts in front of an update's stamp, and in front of nothing else. */
const val UPDATED_MARKER = "编辑于"

/**
 * The unit each of those words stands for.
 *
 * Only the ones this has been seen to use are here, and a stamp worded with anything else is left
 * unread rather than guessed at -- the footer then falls back to the server's own words.
 */
private val AGE_UNITS = mapOf(
    "秒" to ContentAgeUnit.Second,
    "分钟" to ContentAgeUnit.Minute,
    "小时" to ContentAgeUnit.Hour,
    "天" to ContentAgeUnit.Day,
    "周" to ContentAgeUnit.Week,
    "个月" to ContentAgeUnit.Month,
    "月" to ContentAgeUnit.Month,
    "年" to ContentAgeUnit.Year,
)

/**
 * Reads a stamp as the server wrote it, marker and all.
 *
 * @param text The stamp, which an update carries [UPDATED_MARKER] in front of.
 * @return What it says, or null where it is neither a date nor an age this can read.
 */
internal fun parseContentStamp(text: String?): ContentStamp? {
    val body = text?.removePrefix(UPDATED_MARKER)?.trim().orEmpty()
    if (body.isEmpty()) return null

    return parseDate(body) ?: parseAge(body)
}

private fun parseDate(body: String): ContentStamp? =
    runCatching { ContentStamp.At(LocalDateTime.parse(body, DATE_STAMP)) }.getOrNull()

private fun parseAge(body: String): ContentStamp? {
    if (body == JUST_NOW) return ContentStamp.JustNow

    val match = AGE_STAMP.matchEntire(body) ?: return null
    val count = match.groupValues[1].toIntOrNull() ?: return null
    val unit = AGE_UNITS[match.groupValues[2]] ?: return null

    return ContentStamp.Ago(count, unit)
}
