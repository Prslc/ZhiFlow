package com.prslc.zhiflow.core.network

import android.content.res.Resources
import android.os.Build
import com.prslc.zhiflow.BuildConfig
import com.prslc.zhiflow.Natives

object HeaderProvider {
    const val API_VERSION = "3.0.93"
    const val APP_VERSION = "10.73.0"
    const val ZSE_93 = "101_1_1.0"
    private const val ZHIHU_VERSION_CODE = "27314"

    fun zse96(auth: String, urlPath: String): String {
        val signStr = "$ZSE_93+$urlPath+${APP_VERSION}+$auth+${BuildConfig.x_udid}"
        val zse96 = Natives.zse96Sign(signStr)
        return zse96
    }

    val xAppZa: String by lazy {
        val dm = Resources.getSystem().displayMetrics

        val params = linkedMapOf(
            "OS" to "Android",
            "Release" to Build.VERSION.RELEASE,
            "Model" to Build.MODEL,
            "VersionName" to APP_VERSION,
            "VersionCode" to ZHIHU_VERSION_CODE,
            "Product" to "com.zhihu.android",
            "Width" to "${dm.widthPixels}",
            "Height" to "${dm.heightPixels}",
            "Installer" to "Market",
            "DeviceType" to "AndroidPhone",
            "Brand" to Build.BRAND
        )

        params.entries.joinToString("&") { "${it.key}=${it.value}" }
    }
}