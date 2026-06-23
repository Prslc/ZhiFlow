package com.prslc.zhiflow.core.network

import android.content.res.Resources
import android.os.Build
import com.prslc.zhiflow.BuildConfig
import com.prslc.zhiflow.Natives

object HeaderProvider {
    const val API_VERSION = "3.0.93"
    const val APP_VERSION = "10.73.0"
    const val ZSE_93 = "101_1_1.0"
    const val UA = "com.zhihu.android/Futureve/10.73.0 Mozilla/5.0 (Linux; Android 14; 22021211RC Build/UKQ1.231207.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/141.0.7390.122 Mobile Safari/537.36"
    private const val ZHIHU_VERSION_CODE = "27314"

    /**
     * Generate the `x-zse-96` request signature.
     *
     * Signs the concatenation of ZSE protocol version, URL path, app version,
     * authorization token, and device UDID using the native encrypt library.
     */
    fun zse96( urlPath: String, auth: String, xUdid: String): String {
        val signStr = "$ZSE_93+$urlPath+${APP_VERSION}+$auth+$xUdid"
        return Natives.zse96Sign(signStr)
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
