package com.prslc.zhiflow.ui.component.widget

import android.view.View
import android.view.ViewParent
import android.view.Window
import android.widget.Toast
import androidx.activity.compose.LocalActivity
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.utils.platform.ImageHelper
import com.prslc.zhiflow.data.model.content.ZhihuImage
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
/**
 * A full-screen image viewer. Its system bars belong to the dialog's own window rather than the
 * activity's, and fall back to the activity's when there is none (a preview, say).
 *
 * @param images The images to page through. An empty list draws nothing at all.
 * @param initialIndex The page to open on. It is clamped into range, so an index left over from a
 *   list that has since shrunk is safe.
 * @param onDismiss Called when the reader asks to close it: a back, a swipe, or the dialog's own
 *   dismissal.
 * @param modifier Applied to the full-screen `Box` that holds the pager, ahead of the black
 *   background it paints itself.
 */
@Composable
fun ImageLightbox(
    images: List<ZhihuImage>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (images.isEmpty()) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val pagerState = rememberPagerState(
            initialPage = initialIndex.coerceIn(0, (images.size - 1).coerceAtLeast(0))
        ) { images.size }
        var isCurrentPageZoomed by remember { mutableStateOf(false) }
        var isMenuExpanded by remember { mutableStateOf(false) }

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

        val view = LocalView.current
        val activityWindow = LocalActivity.current?.window
        val window = remember(view, activityWindow) {
            view.findDialogWindow() ?: activityWindow
        }
        val insetsController = remember(window) {
            window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        }
        val defaultDarkIcons = !isSystemInDarkTheme()

        val barsType =
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()

        DisposableEffect(insetsController) {
            insetsController?.isAppearanceLightStatusBars = false
            onDispose {
                // Show both bars, not just status: dismissing while zoomed would leave the nav bar
                // hidden.
                insetsController?.show(barsType)
                insetsController?.isAppearanceLightStatusBars = defaultDarkIcons
            }
        }

        LaunchedEffect(isCurrentPageZoomed, insetsController) {
            val controller = insetsController ?: return@LaunchedEffect
            if (isCurrentPageZoomed) {
                controller.hide(barsType)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(barsType)
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp,
                userScrollEnabled = !isCurrentPageZoomed
            ) { pageIndex ->
                val url = images[pageIndex].displayUrl ?: return@HorizontalPager

                val zoomableImageState = rememberZoomableImageState()

                if (pagerState.currentPage == pageIndex) {
                    val zoomed by remember {
                        derivedStateOf {
                            (zoomableImageState.zoomableState.zoomFraction ?: 0f) > 0.01f
                        }
                    }
                    LaunchedEffect(zoomed) { isCurrentPageZoomed = zoomed }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ZoomableAsyncImage(
                        model = ImageRequest.Builder(context).data(url)
                            .crossfade(true).build(),
                        contentDescription = stringResource(R.string.lightbox_image_desc),
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
                        LoadingIndicator(
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
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            isMenuExpanded = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = moreText,
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text(shareText) }, leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share, contentDescription = null
                                )
                            }, onClick = {
                                isMenuExpanded = false
                                scope.launch {
                                    val currentUrl = images[pagerState.currentPage].displayUrl
                                    if (currentUrl != null &&
                                        ImageHelper.shareImage(context, currentUrl).isFailure
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                        Toast.makeText(
                                            appContext, failedText, Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            })

                            DropdownMenuItem(text = { Text(saveActionText) }, leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Save, contentDescription = null
                                )
                            }, onClick = {
                                isMenuExpanded = false
                                scope.launch {
                                    val currentUrl = images[pagerState.currentPage].displayUrl
                                    val saved = currentUrl != null &&
                                        ImageHelper.saveImageToGallery(
                                            appContext, currentUrl
                                        ).isSuccess
                                    haptic.performHapticFeedback(
                                        if (saved) HapticFeedbackType.Confirm
                                        else HapticFeedbackType.Reject
                                    )
                                    val message =
                                        if (saved) successText else failedText
                                    Toast.makeText(appContext, message, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            })
                        }
                    }
                }
            }

            if (images.size > 1 && !isCurrentPageZoomed) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${images.size}",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// The Dialog window hosting this view, or null when it isn't inside one.
private fun View.findDialogWindow(): Window? {
    if (this is DialogWindowProvider) return window
    var parent: ViewParent? = this.parent
    while (parent != null) {
        if (parent is DialogWindowProvider) return parent.window
        parent = parent.parent
    }
    return null
}
