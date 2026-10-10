package com.prslc.zhiflow.core.native

/** JNI bridge to the native `encrypt` library: request signing and request-body sealing. */
object Natives {
    init {
        System.loadLibrary("encrypt")
    }

    /**
     * Sign a request string using the native encryption library.
     *
     * @param signStr The raw string to sign (ZSE protocol + URL + auth + UDID)
     * @return The signed output used as the `x-zse-96` header value
     */
    external fun zse96Sign(signStr: String): String

    /**
     * Seal a form string into a request body, using the native encryption library.
     *
     * @param form The URL-encoded form string built from the endpoint's parameters
     * @return The sealed body — it is sent whole, not as one field among others
     */
    external fun sealBody(form: String): String

    /**
     * Signs the account sign-in message. The secret stays in the library, so this is the only
     * way to produce that signature.
     *
     * @param message The message to sign, exactly as the caller composed it
     * @return The signature, 40 lowercase hexadecimal characters
     */
    external fun authSignature(message: String): String
}
