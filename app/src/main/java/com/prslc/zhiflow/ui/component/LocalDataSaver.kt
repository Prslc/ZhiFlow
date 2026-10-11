package com.prslc.zhiflow.ui.component

import android.annotation.SuppressLint
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the reader has asked to save data, which leaves the images a piece of content carries
 * unloaded.
 *
 * For an illustration in a body and a list's thumbnails, which are the same thing to a reader
 * paying for the traffic. What it does not reach is what is not content: avatars, covers,
 * formulas, and the lightbox, which exists to show an image that was asked for.
 *
 * True leaves each image the box it would have filled, empty and still tappable.
 */
@SuppressLint("ComposeCompositionLocalUsage")
val LocalDataSaver = staticCompositionLocalOf { false }
