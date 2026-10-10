package com.prslc.zhiflow.core.native

/** JNI bridge to the native `encrypt` library. */
object Natives {
    init {
        System.loadLibrary("encrypt")
    }

    /**
     * Signs a request string with the native library.
     *
     * @param signStr The string the caller assembled for it
     * @return What goes in the signature header
     */
    external fun zse96Sign(signStr: String): String

    /**
     * Seals a form string into a request body with the native library.
     *
     * @param form The URL-encoded form string built from the endpoint's parameters
     * @return The body to send — sent whole, not as one field among others
     */
    external fun sealBody(form: String): String

    /**
     * Signs an account message with the native library. The key stays there, so this is the
     * only way to produce that signature.
     *
     * @param message The message to sign, exactly as the caller composed it
     * @return The signature, 40 lowercase hexadecimal characters
     */
    external fun authSignature(message: String): String
}
