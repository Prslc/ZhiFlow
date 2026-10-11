package com.prslc.zhiflow.core.network

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.webkit.WebSettings
import androidx.core.content.edit
import com.prslc.zhiflow.core.native.Natives
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The providers a device can have installed, in the order one of them is looked for. */
private val WEBVIEW_PACKAGES = listOf("com.google.android.webview", "com.android.webview")

private const val KEY_UA = "webview_user_agent"
private const val KEY_UA_VERSION = "webview_user_agent_version"

private val FALLBACK_UA: String =
    "com.zhihu.android/Futureve/${HeaderProvider.APP_VERSION} " +
        "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; wv)"

object HeaderProvider {
    const val API_VERSION = "3.0.93"
    const val APP_VERSION = "10.73.0"
    const val ZSE_93 = "101_1_1.0"
    private const val ZHIHU_VERSION_CODE = "27314"

    // Its own scope, not a caller's: the read has to outlive application startup, and nothing
    // waits on it.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * The dynamic User-Agent string: Zhihu's own prefix and the system's WebView agent.
     *
     * Volatile because the one read that resolves it runs off the main thread, while requests
     * already going out read it from OkHttp's.
     */
    @Volatile
    var UA: String = FALLBACK_UA
        private set

    /**
     * Settles the User-Agent without waiting for the system's WebView.
     *
     * Asking a [WebSettings] for that string loads the WebView provider into this process --
     * most of a cold start, for a string from a subsystem this app otherwise refuses to render
     * in. So the answer is kept, and the asking happens only when the provider has moved since
     * it was written down. Until then [UA] is [FALLBACK_UA], which is what a first run uses.
     *
     * The provider is compared by the installed package's version, read from the package
     * manager rather than from WebView: asking WebView would be the load this avoids.
     *
     * @param context The application context, for the package manager and the WebView agent.
     * @param preferences Where the settled string and the provider version it came from are
     *   kept. Shared with the app's other settings rather than given a file of its own.
     */
    fun init(context: Context, preferences: SharedPreferences) {
        val cached = preferences.getString(KEY_UA, null)
        val cachedVersion = preferences.getString(KEY_UA_VERSION, null)
        UA = cached ?: FALLBACK_UA

        // A provider that cannot be versioned is one this cannot tell has changed, so it is
        // read again -- on the same background thread, so a cold start is not held up either way.
        val version = webViewVersion(context)
        if (cached != null && version != null && version == cachedVersion) return
        refresh(context, preferences, version)
    }

    /** Reads the system's WebView agent and keeps it, off whatever thread called [init]. */
    private fun refresh(context: Context, preferences: SharedPreferences, version: String?) {
        scope.launch {
            val agent = runCatching {
                WebSettings.getDefaultUserAgent(context)
            }.getOrNull() ?: return@launch

            UA = "com.zhihu.android/Futureve/$APP_VERSION $agent"
            preferences.edit {
                putString(KEY_UA, UA)
                putString(KEY_UA_VERSION, version)
            }
        }
    }

    /** @return The version of the WebView provider installed, or null when none is named above. */
    private fun webViewVersion(context: Context): String? {
        val manager = context.packageManager
        return WEBVIEW_PACKAGES.firstNotNullOfOrNull { name ->
            runCatching {
                manager.getPackageInfo(name, PackageManager.PackageInfoFlags.of(0))
            }.getOrNull()?.versionName
        }
    }

    /**
     * Produces the signature header a request to [urlPath] has to carry.
     *
     * @param urlPath The path and query the request will use, exactly as it will be sent
     * @param auth The value of the `Authorization` header
     * @param xUdid The device id
     */
    fun zse96(urlPath: String, auth: String, xUdid: String): String {
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
