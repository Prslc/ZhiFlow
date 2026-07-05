package com.prslc.zhiflow.ui.component.widget

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun CustomBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }

    LaunchedEffect(visible) {
        transitionState.targetState = visible
    }

    LaunchedEffect(transitionState.currentState, transitionState.targetState) {
        if (!transitionState.targetState && !transitionState.currentState && visible) {
            onDismissRequest()
        }
    }

    if (visible || transitionState.currentState) {
        BackHandler {
            transitionState.targetState = false
        }
    }

    AnimatedVisibility(
        visibleState = transitionState,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(250)),
        modifier = modifier.zIndex(100f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { transitionState.targetState = false })
                    }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(350, easing = EaseOutQuart)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(250, easing = EaseInCubic)
                        )
                    )
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                content()
            }
        }
    }
}
