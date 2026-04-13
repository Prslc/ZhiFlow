package com.prslc.zhiflow.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCount(count: Int): String {
    return if (count >= 10000) {
        "%.1fw".format(count / 10000f)
    } else {
        count.toString()
    }
}

fun formatToDate(createdTime: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val millis = createdTime * 1000
    return dateFormat.format(Date(millis))
}

fun String.cleanLatex(): String {
    return this
        .replace("\\,", " ")
        .replace("\\;", " ")
        .replace("\\{", "\\lbrace ")
        .replace("\\}", "\\rbrace ")
        .replace("\\mid", " | ")
        .trimEnd('\\')
}