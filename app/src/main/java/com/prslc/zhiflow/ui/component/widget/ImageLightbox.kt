package com.prslc.zhiflow.ui.component.widget

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.utils.ImageHelper
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

@Composable
fun ImageLightbox(
    imageUrls: List<String>, initialIndex: Int, modifier: Modifier = Modifier, onDismiss: () -> Unit
) {
    if (imageUrls.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (imageUrls.size - 1).coerceAtLeast(0))
    ) { imageUrls.size }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val successText = stringResource(R.string.lightbox_image_save_success)
    val failedText = stringResource(R.string.lightbox_image_save_failed)
    val shareText = stringResource(R.string.lightbox_action_share)
    val saveActionText = stringResource(R.string.lightbox_action_save)

    val backText = stringResource(R.string.general_back)
    val moreText = stringResource(R.string.general_more)

    val isDarkTheme = isSystemInDarkTheme()

    val view = LocalView.current
    val activityWindow = remember(view) {
        (view.context as? Activity)?.window
    }

    val barsType = WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()

    LaunchedEffect(isCurrentPageZoomed, activityWindow) {
        activityWindow?.let { window ->
            val controller = WindowCompat.getInsetsController(window, view)
            if (isCurrentPageZoomed) {
                controller.hide(barsType)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(barsType)
                controller.isAppearanceLightStatusBars = false
            }
        }
    }

    DisposableEffect(activityWindow) {
        onDispose {
            activityWindow?.let { window ->
                val controller = WindowCompat.getInsetsController(window, view)
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.isAppearanceLightStatusBars = !isDarkTheme
            }
        }
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .zIndex(200f)
    ) {
        var isMenuExpanded by remember { mutableStateOf(false) }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            pageSpacing = 16.dp,
            userScrollEnabled = !isCurrentPageZoomed
        ) { pageIndex ->
            val url = imageUrls[pageIndex]

            val zoomableImageState = rememberZoomableImageState(rememberZoomableState())

            if (pagerState.currentPage == pageIndex) {
                val zoomed by remember {
                    derivedStateOf {
                        (zoomableImageState.zoomableState.zoomFraction ?: 0f) > 0.01f
                    }
                }
                LaunchedEffect(zoomed) { isCurrentPageZoomed = zoomed }
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ZoomableAsyncImage(
                    model = ImageRequest.Builder(context).data(url).size(Size.ORIGINAL)
                        .crossfade(true).build(),
                    contentDescription = "Lightbox Page $pageIndex",
                    state = zoomableImageState,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    onClick = {
                        if ((zoomableImageState.zoomableState.zoomFraction ?: 0f) <= 0.01f) {
                            onDismiss()
                        }
                    },
                )

                if (!zoomableImageState.isImageDisplayed) {
                    CircularProgressIndicator(
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.statusBarsPadding(),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isCurrentPageZoomed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f), Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = backText,
                        tint = Color.White
                    )
                }

                Box {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        isMenuExpanded = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = moreText,
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded, onDismissRequest = { isMenuExpanded = false }) {
                        // Share Image
                        DropdownMenuItem(text = { Text(shareText) }, leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Share, contentDescription = null
                            )
                        }, onClick = {
                            isMenuExpanded = false
                            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            scope.launch {
                                val currentUrl = imageUrls[pagerState.currentPage]
                                val shareResult = ImageHelper.shareImage(context, currentUrl)
                                if (shareResult.isFailure) {
                                    Toast.makeText(
                                        appContext, failedText, Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        })

                        // Save Image
                        DropdownMenuItem(text = { Text(saveActionText) }, leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Save, contentDescription = null
                            )
                        }, onClick = {
                            isMenuExpanded = false
                            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            scope.launch {
                                val currentUrl = imageUrls[pagerState.currentPage]
                                val result = ImageHelper.saveImageToGallery(
                                    appContext, currentUrl
                                )
                                val message = if (result.isSuccess) successText else failedText
                                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }
        }

        // page number
        if (imageUrls.size > 1 && !isCurrentPageZoomed) {
            Text(
                text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
