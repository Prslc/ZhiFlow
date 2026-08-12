package com.prslc.zhiflow.core.network

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.webkit.WebSettings
import com.prslc.zhiflow.core.native.AppIntegrity
import com.prslc.zhiflow.core.native.Natives

object HeaderProvider {
    const val API_VERSION = "3.0.93"
    const val APP_VERSION = "10.73.0"
    const val ZSE_93 = "101_1_1.0"
    private const val ZHIHU_VERSION_CODE = "27314"

    /**
    * The dynamic User-Agent string combined with Zhihu's app-specific prefix and the
    * system's native WebView user agent.
    *
    * Defaults to a pre-constructed fallback matching the current device specifications
    * to prevent crashes before initialization.
    */
    var UA: String = "com.zhihu.android/Futureve/$APP_VERSION Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; wv)"
        private set

    /**
    * Initializes the dynamic User-Agent by fetching the system's underlying WebView UA.
    *
    * **Note:** This method must be invoked eagerly at application startup (e.g., via Koin's
    * `createdAtStart` scope) to ensure [UA] is fully populated before any network requests are dispatched.
    *
    * @param context The application context used to resolve [WebSettings].
    */
    fun init(context: Context) {
        val systemWebViewUa = try {
            WebSettings.getDefaultUserAgent(context)
        } catch (e: Exception) {
            "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; wv)"
        }
        UA = "com.zhihu.android/Futureve/$APP_VERSION $systemWebViewUa"
    }

    /**
     * Generate the `x-zse-96` request signature.
     *
     * Signs the concatenation of ZSE protocol version, URL path, app version,
     * authorization token, and device UDID using the native encrypt library.
     */
    fun zse96( urlPath: String, auth: String, xUdid: String): String {
        val signStr = "$ZSE_93+$urlPath+${APP_VERSION}+$auth+$xUdid"
        return Natives.zse96Sign(signStr, AppIntegrity.signingCertSha256)
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
