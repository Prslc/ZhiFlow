package com.prslc.zhiflow.ui.component.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prslc.zhiflow.data.model.content.Formula as ZhihuFormula
import com.prslc.zhiflow.data.model.content.ZhihuImage
import org.koin.compose.koinInject

/**
 * One picture the lightbox can show: an image, or a block formula. An inline formula is neither --
 * it belongs to the line of text it sits in, and has no page of its own.
 *
 * The two kinds are not interchangeable on the screen -- a formula is drawn against the surface
 * rather than the theme, and it carries the LaTeX behind it, which is the only form of it that can
 * be copied -- so a page asks which it is instead of being handed a url.
 */
@Immutable
sealed interface LightboxItem {
    /** The bitmap this page zooms and hands to share and save, or null for one that has none. */
    val imageUrl: String?

    /** An image the API sent. */
    data class Image(val image: ZhihuImage) : LightboxItem {
        override val imageUrl: String? get() = image.displayUrl
    }

    /** A block formula: [formula] carries both the bitmap and the LaTeX the page can copy. */
    data class Formula(val formula: ZhihuFormula) : LightboxItem {
        override val imageUrl: String? get() = formula.imgUrl
    }
}

/**
 * The one lightbox a screen has: what is open, and which page of it.
 *
 * Widgets open it by handing over their pictures and the instance that was tapped;
 * [ImageLightboxHost] draws whatever it holds. A screen that shows pictures therefore holds no
 * lightbox state of its own.
 */
class ImageLightboxController {

    @Immutable
    data class State(
        val items: List<LightboxItem>,
        val initialIndex: Int,
    )

    var state by mutableStateOf<State?>(null)
        private set

    /**
     * Opens the lightbox on [tapped], with the rest of [items] paged around it.
     *
     * The page is found by identity and not by equality: one document can show the same picture
     * twice, and matching by value would land on the first of them. A [tapped] that is not one of
     * [items]' own instances opens nothing at all.
     *
     * Both sides of that are the caller's to keep: [items] is built where the tap is handled and
     * not remembered across the composition, so that a list built before the content was last
     * parsed cannot outlive it and take the tap with it.
     *
     * @param items The document's pictures, in the order they are drawn.
     * @param tapped The image the reader pressed; it has to come from [items] itself.
     */
    fun open(items: List<LightboxItem>, tapped: ZhihuImage) {
        val index = items.indexOfFirst { it is LightboxItem.Image && it.image === tapped }
        openAt(items, index)
    }

    /**
     * Opens the lightbox on [tapped], under the same terms as the image overload.
     *
     * @param items The document's pictures, in the order they are drawn.
     * @param tapped The formula the reader pressed; it has to come from [items] itself.
     */
    fun open(items: List<LightboxItem>, tapped: ZhihuFormula) {
        val index = items.indexOfFirst { it is LightboxItem.Formula && it.formula === tapped }
        openAt(items, index)
    }

    /** Closes the lightbox. A no-op when nothing is open. */
    fun dismiss() {
        state = null
    }

    private fun openAt(items: List<LightboxItem>, index: Int) {
        if (index == -1) return
        state = State(items, index)
    }
}

/**
 * Draws the lightbox whenever the controller has one open, and nothing otherwise.
 *
 * Place it once per screen, outside the content that opens it.
 *
 * @param modifier Applied to the lightbox itself.
 */
@Composable
fun ImageLightboxHost(modifier: Modifier = Modifier) {
    val controller = koinInject<ImageLightboxController>()
    controller.state?.let { state ->
        ImageLightbox(
            items = state.items,
            initialIndex = state.initialIndex,
            onDismiss = controller::dismiss,
            modifier = modifier,
        )
    }
}

/**
 * A one-page lightbox trigger for a standalone image, such as an avatar or a cover.
 *
 * @param url The image to open, or null or blank for a trigger that does nothing.
 * @return The trigger to call from a tap. It is a lambda rather than the action itself, so a caller
 *   that never calls it never opens anything.
 */
@Composable
fun rememberSingleImageLightbox(url: String?): () -> Unit {
    val controller = koinInject<ImageLightboxController>()
    val image = remember(url) {
        url?.takeIf { it.isNotBlank() }?.let { nonBlank ->
            ZhihuImage(
                urls = listOf(nonBlank),
                width = 0,
                height = 0,
                description = "",
                isGif = false,
            )
        }
    }
    val items = remember(image) { image?.let { listOf(LightboxItem.Image(it)) }.orEmpty() }
    val first = image ?: return {}
    return { controller.open(items, first) }
}
