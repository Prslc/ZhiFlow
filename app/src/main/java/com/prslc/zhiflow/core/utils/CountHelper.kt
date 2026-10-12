package com.prslc.zhiflow.core.utils

import java.util.Locale

/** The scales a language that counts in ten-thousands divides by, largest first. */
private val CHINESE_SCALES = listOf(
    100_000_000 to "亿",
    10_000 to "万",
)

/** The same for a language that counts in thousands and millions. */
private val ENGLISH_SCALES = listOf(
    1_000_000_000 to "B",
    1_000_000 to "M",
    1_000 to "K",
)

/**
 * A count as the reader's own language writes a large one: `17.4 万` in Chinese, `174.2K` in
 * English. The two languages divide at different places, so this is two scales rather than one
 * with its unit swapped; one landing exactly on its scale takes no decimal, and one reaching
 * no scale at all is written as it stands.
 *
 * The language comes from the locale rather than from a string resource: this is not a
 * composable, and the unit is part of how the number is spelled. The same locale goes to the
 * format as well, so the decimal mark belongs to that language too.
 *
 * @param count The count to write.
 * @return It, abbreviated where the language abbreviates and carrying that language's unit.
 */
internal fun formatCount(count: Int): String {
    val locale = Locale.getDefault()
    val scales = if (locale.language == "zh") CHINESE_SCALES else ENGLISH_SCALES

    // Largest scale first: the first one the count reaches is the one it is written in.
    for ((divisor, suffix) in scales) {
        if (count >= divisor) {
            val value = count.toFloat() / divisor
            return if (count % divisor == 0) {
                "${value.toInt()}$suffix"
            } else {
                "%.1f$suffix".format(locale, value)
            }
        }
    }

    return count.toString()
}
