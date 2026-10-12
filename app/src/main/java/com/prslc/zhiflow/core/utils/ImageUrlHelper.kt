package com.prslc.zhiflow.core.utils

/**
 * What each size the CDN names a picture with comes to, in pixels.
 *
 * A picture is kept at several sizes, named by a suffix on its file name, and the server hands out
 * whichever one it sees fit — a small one for an avatar, a middle one for a body. The wide ones are
 * named for their width; the rest are the presets the others use.
 */
private val IMAGE_SUFFIX_PIXELS = mapOf(
    "_l" to 100,
    "_xl" to 200,
    "_qhd" to 480,
    "_b" to 600,
    "_hd" to 720,
    "_720w" to 720,
    "_1080w" to 1170,
    "_1440w" to 1170,
    "_xxl" to 1249,
    "_r" to 1249,
)

/**
 * The sizes to move a picture up to, smallest first.
 *
 * Each of them was found on both an avatar and a body image — the CDN keeps these for both — so
 * moving a picture to one of them cannot move it to a file that is not there.
 */
private val IMAGE_STEPS = listOf(200 to "_xl", 600 to "_b", 1249 to "_xxl")

/**
 * The same picture, asked for at the size it is about to be drawn at.
 *
 * A picture drawn larger than the size handed over is upscaled, which is what leaves a wide avatar
 * or a full-screen image looking soft. The sizes are a suffix away from one another, so asking for
 * the next one up costs a string replace — the server's own client does the same, and never asks
 * for the little presets.
 *
 * Sizes only ever move up, and only off the suffixes listed above: a url this does not recognise is
 * left alone and drawn as it came, rather than rewritten into something the CDN may not keep.
 *
 * @param url The url the server handed out.
 * @param widthPx How wide the picture will be drawn, in pixels.
 * @return The same url at that size or at the next size up from it, or [url] unchanged.
 */
internal fun imageUrlAt(url: String, widthPx: Int): String {
    val suffix = IMAGE_SUFFIX_PIXELS.keys.firstOrNull { url.contains("$it.") } ?: return url
    if (IMAGE_SUFFIX_PIXELS.getValue(suffix) >= widthPx) return url

    val step = IMAGE_STEPS.firstOrNull { it.first >= widthPx }?.second ?: IMAGE_STEPS.last().second
    return url.replaceFirst("$suffix.", "$step.")
}
