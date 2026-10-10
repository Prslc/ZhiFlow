package com.prslc.zhiflow.core.network

import okhttp3.OkHttpClient

/**
 * What a service makes its API calls through.
 *
 * The funnel is an inline extension, so anything it needs on every call has to hang off its
 * receiver.
 *
 * @param gateProvider Resolved on first use: the gate needs a client of its own, so the two
 *   cannot be built in either order.
 */
class ApiClient(
    val http: OkHttpClient,
    gateProvider: () -> SessionGate,
) {
    internal val gate: SessionGate by lazy(gateProvider)
}
