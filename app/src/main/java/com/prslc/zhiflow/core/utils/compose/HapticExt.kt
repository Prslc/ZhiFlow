package com.prslc.zhiflow.core.utils.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Returns a lambda that confirms a toggle by feel: [HapticFeedbackType.ToggleOn] when the control
 * lands in its active state, [HapticFeedbackType.ToggleOff] when it leaves it.
 *
 * Pass the state the control is *becoming*, not the one it is leaving.
 */
@Composable
internal fun rememberToggleHaptic(): (active: Boolean) -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) {
        { active ->
            haptic.performHapticFeedback(
                if (active) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
            )
        }
    }
}
