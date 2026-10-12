package com.prslc.zhiflow.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * A preview in both modes.
 *
 * Two `@Preview` annotations on one composed annotation, which is the supported way to say "draw
 * this twice": the mode reaches the component through [PreviewSurface], so a preview function
 * carries no mention of light or dark at all and the two can never drift apart.
 *
 * It is worth the second render here because the app draws its own palette rather than the
 * scheme's in places -- the syntax colors of a code block, the scrims over a cover -- and a color
 * that clears contrast in one mode does not always clear it in the other.
 */
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class AppPreviews
