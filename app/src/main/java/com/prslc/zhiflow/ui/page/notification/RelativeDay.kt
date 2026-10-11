package com.prslc.zhiflow.ui.page.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.prslc.zhiflow.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * When something arrived, the way a list of conversations and notifications writes it: the day
 * itself, the day before, the date as the reader's locale writes it, and the year as well once it
 * is no longer this one.
 *
 * @param createdTime When it arrived, in seconds.
 * @return What to draw beside it.
 */
@Composable
@ReadOnlyComposable
internal fun relativeDay(createdTime: Long): String {
    val date = Instant.ofEpochSecond(createdTime).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()

    return when (date) {
        today -> stringResource(R.string.time_today)

        today.minusDays(1) -> stringResource(R.string.time_yesterday)

        else -> date.format(
            DateTimeFormatter.ofPattern(if (date.year == today.year) "MM-dd" else "yyyy-MM-dd"),
        )
    }
}
