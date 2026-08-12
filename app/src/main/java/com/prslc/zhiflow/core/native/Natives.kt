package com.prslc.zhiflow.core.native

/** JNI bridge to the native `encrypt` library for Zhihu request signing. */
object Natives {
    init {
        System.loadLibrary("encrypt")
    }

    /**
     * Sign a request string using the native encryption library.
     *
     * @param signStr The raw string to sign (ZSE protocol + URL + auth + UDID)
     * @param signingCertSha256 SHA-256 of the installed APK's signing certificate
     * @return The signed output used as the `x-zse-96` header value
     */
    external fun zse96Sign(signStr: String, signingCertSha256: ByteArray): String
}