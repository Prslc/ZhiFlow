package com.prslc.zhiflow.core.native

/** JNI bridge to the native `encrypt` library. */
object Natives {
    init {
        System.loadLibrary("encrypt")
    }

    /**
     * Loads the library here and now, on the thread that calls this, rather than whenever
     * something first needs the bridge.
     *
     * The body is empty on purpose: the class's own `init` is the work, so merely naming the
     * object is the whole effect, and this function is what gives that a name to call.
     *
     * It exists because the load goes through `Runtime.loadLibrary0`, which is a `synchronized`
     * method on the process's one `Runtime` — every library, on every thread, queues on that one
     * monitor. A cold start that reaches this bridge from a worker while the main thread is
     * loading a library of its own makes one of the two wait for the other, and the main thread
     * is the one that cannot afford it: it was measured waiting there for between 23 and 80 ms
     * across cold starts.
     *
     * The call has to stay ahead of everything that could touch this class, a `ContentProvider`'s
     * `onCreate` included: those run before the application's, and one that reached the bridge
     * from a background thread first would bring the race back silently, with this call already
     * too late to do anything about it.
     */
    fun preload() = Unit

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
