package com.prslc.zhiflow.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatCount(count: Int): String {
    return if (count >= 10000) {
        "%.1fw".format(count / 10000f)
    } else {
        count.toString()
    }
}

internal fun formatToDate(createdTime: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val millis = createdTime * 1000
    return dateFormat.format(Date(millis))
}
