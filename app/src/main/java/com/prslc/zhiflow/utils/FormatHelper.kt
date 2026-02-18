package com.prslc.zhiflow.utils

fun formatCount(count: Int): String {
    return if (count >= 10000) {
        "%.1fw".format(count / 10000f)
    } else {
        count.toString()
    }
}