package com.prslc.zhiflow.ui.component

import android.widget.Toast
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.prslc.zhiflow.R
import com.prslc.zhiflow.utils.ImageSaveHelper
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

@Composable
fun ImageLightbox(
    imageUrl: String?,
    onDismiss: () -> Unit
) {
    if (imageUrl == null) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val zoomableImageState = rememberZoomableImageState(rememberZoomableState())
    val successText = stringResource(R.string.lightbox_image_save_success)
    val failedText = stringResource(R.string.lightbox_image_save_failed)

    val isDarkTheme = isSystemInDarkTheme()

    val isZoomed by remember {
        derivedStateOf { (zoomableImageState.zoomableState.zoomFraction ?: 0f) > 0.01f }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogView = LocalView.current
        val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window

        val barsType = WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()

        // reset zoom
        LaunchedEffect(imageUrl) {
            zoomableImageState.zoomableState.resetZoom(animationSpec = androidx.compose.animation.core.snap())
        }

        LaunchedEffect(isZoomed, dialogWindow) {
            dialogWindow?.let { window ->
                val controller = WindowCompat.getInsetsController(window, dialogView)
                if (isZoomed) {
                    // Hide status bars during zoom for immersive viewing with swipe-to-show behavior
                    controller.hide(barsType)
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    // Restore status bars and ensure icon visibility when image is fit to screen
                    controller.show(barsType)
                    controller.isAppearanceLightStatusBars = false
                }
            }
        }

        DisposableEffect(dialogWindow) {
            onDispose {
                // Force light status bar icons for the dark background and restore system defaults on exit
                dialogWindow?.let { window ->
                    val controller = WindowCompat.getInsetsController(window, dialogView)
                    controller.show(WindowInsetsCompat.Type.statusBars())
                    controller.isAppearanceLightStatusBars = !isDarkTheme
                }
            }
        }

        imageUrl.let { url ->
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
                            if (!isZoomed) {
                                onDismiss()
                            }
                        },
                        onLongClick = {
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

                    // Loading
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
}