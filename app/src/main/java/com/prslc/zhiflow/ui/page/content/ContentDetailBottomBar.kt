package com.prslc.zhiflow.ui.page.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import com.prslc.zhiflow.data.model.ZhihuContent
import com.prslc.zhiflow.ui.component.widget.BottomBar

@Composable
fun ContentDetailBottomBar(
    isVisible: Boolean,
    currentContent: ZhihuContent?,
    interaction: ContentViewModel.InteractionState,
    displayUpvoteCount: Int,
    onVoteClick: (String) -> Unit,
    onStarClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        currentContent?.let { content ->
            BottomBar(
                isUpvoted = interaction.isUpvoted,
                isDownvoted = interaction.isDownvoted,
                isFavorite = interaction.isFavorite,
                upvoteCount = displayUpvoteCount,
                favCount = content.reaction?.statistics?.favoritesCount ?: 0,
                commentCount = content.reaction?.statistics?.commentCount ?: 0,
                onVoteClick = onVoteClick,
                onStarClick = onStarClick,
                onCommentClick = onCommentClick,
            )
        }
    }
}
