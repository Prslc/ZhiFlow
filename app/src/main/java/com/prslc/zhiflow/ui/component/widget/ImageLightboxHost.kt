package com.prslc.zhiflow.ui.component.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prslc.zhiflow.data.model.content.ZhihuImage
import org.koin.compose.koinInject

/**
 * The one lightbox a screen has: what is open, and which page of it.
 *
 * Widgets open it by handing over their images and the instance that was tapped;
 * [ImageLightboxHost] draws whatever it holds. A screen that shows images therefore holds no
 * lightbox state of its own.
 */
class ImageLightboxController {

    @Immutable
    data class State(
        val images: List<ZhihuImage>,
        val initialIndex: Int,
    )

    var state by mutableStateOf<State?>(null)
        private set

    /**
     * Opens the lightbox on [tapped], with the rest of [images] paged around it.
     *
     * The page is found by identity and not by equality: one document can show the same image
     * twice, and matching by value would land on the first of them. A [tapped] that is not one of
     * [images]' own instances opens nothing at all.
     *
     * @param images The document's images, in the order they are drawn.
     * @param tapped The instance the reader pressed; it has to come from [images] itself.
     */
    fun open(images: List<ZhihuImage>, tapped: ZhihuImage) {
        // Identity, not equality: the same image can appear twice in one document.
        val index = images.indexOfFirst { it === tapped }
        if (index == -1) return
        state = State(images, index)
    }

    /** Closes the lightbox. A no-op when nothing is open. */
    fun dismiss() {
        state = null
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
            images = state.images,
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
    val images = remember(url) {
        url?.takeIf { it.isNotBlank() }?.let { nonBlank ->
            listOf(
                ZhihuImage(
                    urls = listOf(nonBlank),
                    width = 0,
                    height = 0,
                    description = "",
                    isGif = false,
                )
            )
        }.orEmpty()
    }
    val first = images.firstOrNull() ?: return {}
    return { controller.open(images, first) }
}
