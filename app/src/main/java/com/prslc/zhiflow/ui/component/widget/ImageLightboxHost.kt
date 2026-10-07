package com.prslc.zhiflow.ui.component.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prslc.zhiflow.data.model.content.ZhihuImage
import org.koin.compose.koinInject

class ImageLightboxController {

    @Immutable
    data class State(
        val images: List<ZhihuImage>,
        val initialIndex: Int,
    )

    var state by mutableStateOf<State?>(null)
        private set

    fun open(images: List<ZhihuImage>, tapped: ZhihuImage) {
        // Identity, not equality: the same image can appear twice in one document.
        val index = images.indexOfFirst { it === tapped }
        if (index == -1) return
        state = State(images, index)
    }

    fun dismiss() {
        state = null
    }
}

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
