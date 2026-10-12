package com.prslc.zhiflow.data.mapper

/** What an author's standing line reports: one count for each of the three it spells out. */
internal data class InfluenceStats(
    val agree: Int,
    val like: Int,
    val favourite: Int,
)

/** Which of those three a count is, while a line is being read. */
private enum class InfluenceKind { AGREE, LIKE, FAVOURITE }

/** The word each count is written with, and which of the three it belongs to. */
private val INFLUENCE_WORDS = mapOf(
    "次赞同" to InfluenceKind.AGREE,
    "次喜欢" to InfluenceKind.LIKE,
    "次收藏" to InfluenceKind.FAVOURITE,
)

/** What the server ends a count written in ten-thousands with, as in `26.5 万次喜欢`. */
private const val TEN_THOUSAND = "万"

/**
 * Reads the standing the server words for a column's author, as counts.
 *
 * The line arrives worded -- `"6046 次赞同"`, `"26.5 万次喜欢"` -- and the words are the reader's
 * own, taken from their strings, so what is read here is what each counts and how many. A line
 * that cannot be read whole is not read at all: the caller keeps the server's own words for it,
 * in place of half a line in ours.
 *
 * @param words The phrases, one per count.
 * @return The three counts, or null where any phrase was not one this can read.
 */
internal fun influenceStatsOf(words: List<String>): InfluenceStats? {
    val counts = mutableMapOf<InfluenceKind, Int>()

    for (word in words) {
        val entry = INFLUENCE_WORDS.entries.firstOrNull { word.endsWith(it.key) } ?: return null
        val rest = word.removeSuffix(entry.key).filterNot { it.isWhitespace() }
        val digits = rest.removeSuffix(TEN_THOUSAND)
        if (digits.toDoubleOrNull() == null) return null

        counts[entry.value] = parseCount(
            value = digits,
            wanSuffix = if (digits.length < rest.length) TEN_THOUSAND else "",
        )
    }

    val agree = counts[InfluenceKind.AGREE] ?: return null
    val like = counts[InfluenceKind.LIKE] ?: return null
    val favourite = counts[InfluenceKind.FAVOURITE] ?: return null

    return InfluenceStats(agree, like, favourite)
}
