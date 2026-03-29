package com.prslc.zhiflow.ui.component

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

    val isZoomed by remember {
        derivedStateOf {
            (zoomableImageState.zoomableState.zoomFraction ?: 0f) > 0.01f
        }
    }

    var activeUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imageUrl) {
        if (imageUrl != null) activeUrl = imageUrl
    }

    AnimatedVisibility(
        visible = imageUrl != null,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(400))
    ) {
        val view = LocalView.current
        val isDarkTheme = isSystemInDarkTheme()
        val urlToShow = imageUrl ?: activeUrl
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)

        DisposableEffect(Unit) {
            // Force light status bar icons for the dark background and restore system defaults on exit
            controller.isAppearanceLightStatusBars = false
            onDispose {
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.isAppearanceLightStatusBars = !isDarkTheme
            }
        }

        LaunchedEffect(isZoomed, imageUrl) {
            if (imageUrl != null) {
                if (isZoomed) {
                    // Hide status bars during zoom for immersive viewing with swipe-to-show behavior
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    // Restore status bars and ensure icon visibility when image is fit to screen
                    controller.show(WindowInsetsCompat.Type.statusBars())
                    controller.isAppearanceLightStatusBars = false
                }
            }
        }

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