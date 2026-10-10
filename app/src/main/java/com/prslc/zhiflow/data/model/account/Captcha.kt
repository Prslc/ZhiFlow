package com.prslc.zhiflow.data.model.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Captcha(
    @SerialName("cookie") val cookie: String = "",
    @SerialName("show_captcha") val showCaptcha: Boolean = false,
)
