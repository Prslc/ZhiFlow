package com.prslc.zhiflow

object Natives {
    init {
        System.loadLibrary("encrypt")
    }
    external fun zse96Sign(signStr: String): String
}
