package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.model.ComponentCard
import com.prslc.zhiflow.ui.page.people.moment.MomentContentType
import com.prslc.zhiflow.ui.page.people.moment.MomentItemState

internal fun ComponentCard.toItemState(): MomentItemState {
    val bem = extra?.businessExtMap
    val contentInfo = bem?.contentInfo

    val realId = contentInfo?.contentId
        ?: contentInfo?.contentToken
        ?: this.id

    val momentType = when (contentInfo?.contentType) {
        "ANSWER" -> MomentContentType.ANSWER
        "ARTICLE" -> MomentContentType.ARTICLE
        "PIN" -> MomentContentType.THOUGHT
        else -> MomentContentType.UNKNOWN
    }

    val resolvedTitle = when (momentType) {
        MomentContentType.ANSWER -> bem?.parentContentData?.contentInfo?.detail?.title
        MomentContentType.ARTICLE -> contentInfo?.detail?.title
        MomentContentType.THOUGHT -> contentInfo?.detail?.title
        else -> bem?.parentContentData?.contentInfo?.detail?.title ?: contentInfo?.detail?.title
    } ?: ""

    val totalVotes = (bem?.reactionMap?.like?.count ?: 0) + (bem?.reactionMap?.voteUp?.count ?: 0)

    return MomentItemState(
        id = realId,
        type = momentType,
        title = resolvedTitle,
        plainContent = contentInfo?.detail?.plainContent ?: "",
        summary = contentInfo?.detail?.summary ?: "",
        authorName = bem?.author?.profile?.fullName ?: "Anonymous user",
        authorAvatarUrl = bem?.author?.profile?.avatar?.url,
        routerUrl = bem?.router,
        voteCount = totalVotes,
        commentCount = bem?.reactionMap?.comment?.count ?: 0,
        collectCount = bem?.reactionMap?.collect?.count ?: 0,
        mediaImages = contentInfo?.mediaDetail?.images ?: emptyList(),
        videoThumbnail = contentInfo?.mediaDetail?.video?.thumbnail
            ?: contentInfo?.mediaDetail?.videos?.firstOrNull()?.thumbnail,
        publishedAt = contentInfo?.publishedAt ?: 0,
        actionText = bem?.momentsBizData?.actionText ?: "",
        actionTime = (bem?.momentsBizData?.actionTimeMs ?: 0) / 1000,
        isTopping = bem?.momentsBizData?.feedType == "topping"
    )
}

