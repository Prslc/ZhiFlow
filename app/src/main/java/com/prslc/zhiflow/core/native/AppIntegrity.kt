package com.prslc.zhiflow.core.native

import android.content.Context
import android.content.pm.PackageManager
import java.security.MessageDigest

/**
 * Supplies the running APK's signing-certificate digest to the native layer,
 * which verifies it against the digests compiled into `libencrypt.so`.
 */
object AppIntegrity {
    private lateinit var appContext: Context

    /** Must be called once at application startup, before any request is dispatched. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * SHA-256 of the installed APK's signing certificate (raw DER bytes).
     *
     * This is the same value the native library was built against: the certificate
     * digest is constant for the lifetime of a signed install, so it is computed
     * lazily once and cached.
     */
    val signingCertSha256: ByteArray by lazy {
        val info = appContext.packageManager.getPackageInfo(
            appContext.packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
        )
        val signer = info.signingInfo?.apkContentsSigners?.firstOrNull()
            ?: throw IllegalStateException("APK has no signing certificate")
        MessageDigest.getInstance("SHA-256").digest(signer.toByteArray())
    }
}
