package com.prslc.zhiflow.ui.page.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import com.prslc.zhiflow.data.model.content.ZhihuContent
import com.prslc.zhiflow.ui.component.widget.BottomBar

@Composable
fun ContentDetailBottomBar(
    isVisible: Boolean,
    currentContent: ZhihuContent?,
    interaction: ContentViewModel.InteractionState,
    upvoteCount: Int?,
    onVoteClick: (String) -> Unit,
    onStarClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
    ) {
        BottomBar(
            isUpvoted = interaction.isUpvoted,
            isDownvoted = interaction.isDownvoted,
            isFavorite = interaction.isFavorite,
            upvoteCount = upvoteCount,
            favCount = currentContent?.reaction?.statistics?.favoritesCount,
            commentCount = currentContent?.reaction?.statistics?.commentCount,
            onVoteClick = onVoteClick,
            onStarClick = onStarClick,
            onCommentClick = onCommentClick,
        )
    }
}
