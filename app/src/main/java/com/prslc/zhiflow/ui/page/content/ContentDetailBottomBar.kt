package com.prslc.zhiflow.ui.page.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.prslc.zhiflow.core.utils.compose.HideOnScrollState
import com.prslc.zhiflow.data.model.content.ZhihuContent
import com.prslc.zhiflow.ui.component.InteractionState
import com.prslc.zhiflow.ui.component.widget.BottomBar

/**
 * The page's own bar, pushed out of the way by the list the way the top bar is: under the finger,
 * rather than on an animation of its own.
 *
 * Nothing sits under it, so it only has to leave -- there is no layout to shorten, and no scroll to
 * take.
 */
@Composable
internal fun ContentDetailBottomBar(
    hide: HideOnScrollState,
    currentContent: ZhihuContent?,
    interaction: InteractionState,
    upvoteCount: Int?,
    onVoteClick: (String) -> Unit,
    onStarClick: () -> Unit,
    onCommentClick: () -> Unit,
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
        // Read while drawing: the fraction is a new value on every frame the list moves.
        modifier = Modifier.graphicsLayer { translationY = size.height * hide.fraction },
    )
}
