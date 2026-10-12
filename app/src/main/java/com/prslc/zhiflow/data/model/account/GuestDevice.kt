package com.prslc.zhiflow.data.model.account

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What the device bootstrap answers: the id this install will be known by, and a session.
 *
 * The session travels as the same shape the guest token does, so [GuestToken] carries it rather
 * than a second class that would have to be kept in step with the first.
 */
@Serializable
data class GuestDevice(
    @SerialName("udid") val udid: String = "",
    @SerialName("guest") val guest: GuestToken = GuestToken(),
)
