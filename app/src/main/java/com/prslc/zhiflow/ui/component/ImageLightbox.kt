package com.prslc.zhiflow.ui.component

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import coil.request.ImageRequest
import coil.size.Size
import com.prslc.zhiflow.R
import com.prslc.zhiflow.utils.ImageSaveHelper
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

@Composable
fun ImageLightbox(
    imageUrl: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val zoomableImageState = rememberZoomableImageState(rememberZoomableState())

    val successText = stringResource(R.string.lightbox_image_save_success)
    val failedText = stringResource(R.string.lightbox_image_save_failed)

    var activeUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(imageUrl) {
        if (imageUrl != null) activeUrl = imageUrl
    }

    AnimatedVisibility(
        visible = imageUrl != null,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(400))
    ) {
        val urlToShow = imageUrl ?: activeUrl

        urlToShow?.let { url ->
            Surface(
                color = Color.Black,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ZoomableAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(url)
                            .size(Size.ORIGINAL)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Lightbox",
                        state = zoomableImageState,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center,
                        onClick = {
                            val currentZoom = zoomableImageState.zoomableState.zoomFraction ?: 0f
                            if (currentZoom <= 0.05f) {
                                onDismiss()
                            }
                        },
                        onLongClick = { _ ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                val result = ImageSaveHelper.saveImageToGallery(context, url)
                                Toast.makeText(
                                    context,
                                    if (result.isSuccess) successText else failedText,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )

                    // loading
                    if (!zoomableImageState.isImageDisplayed) {
                        CircularProgressIndicator(
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.statusBarsPadding()
                        )
                    }
                }
            }
        }
    }

    // reset zoom
    LaunchedEffect(imageUrl == null) {
        if (imageUrl == null) {
            zoomableImageState.zoomableState.resetZoom(animationSpec = snap())
        }
    }
}