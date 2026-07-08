package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.model.moment.ComponentCard
import com.prslc.zhiflow.data.model.moment.MediaImage
import com.prslc.zhiflow.data.model.moment.MomentsFeedItem
import com.prslc.zhiflow.ui.page.people.moment.MomentContentType
import com.prslc.zhiflow.data.dto.MomentDto

internal fun ComponentCard.toDto(): MomentDto {
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

    return MomentDto(
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
        isTopping = bem?.momentsBizData?.feedType == "topping",
    )
}

internal fun MomentsFeedItem.toDto(): MomentDto {
    val target = this.target
    val source = this.source
    val bem = extra?.businessExtMap

    val actionType = source?.actionType ?: bem?.momentsBizData?.actionType

    val momentType = when {
        actionType == "MEMBER_MEMBER_FOLLOW" -> MomentContentType.USER
        actionType == "MEMBER_FOLLOW_QUESTION" -> MomentContentType.UNKNOWN
        target?.type == "answer" -> MomentContentType.ANSWER
        target?.type == "article" -> MomentContentType.ARTICLE
        target?.type == "pin" -> MomentContentType.THOUGHT
        bem?.contentInfo?.contentType == "ANSWER" -> MomentContentType.ANSWER
        bem?.contentInfo?.contentType == "ARTICLE" -> MomentContentType.ARTICLE
        bem?.contentInfo?.contentType == "PIN" -> MomentContentType.THOUGHT
        target?.type == "question" -> MomentContentType.UNKNOWN
        else -> MomentContentType.UNKNOWN
    }

    val title = when (momentType) {
        MomentContentType.USER -> target?.name
        MomentContentType.ANSWER -> target?.question?.title
            ?: bem?.parentContentData?.contentInfo?.detail?.title
        MomentContentType.ARTICLE -> target?.title
            ?: bem?.contentInfo?.detail?.title
        MomentContentType.THOUGHT -> target?.excerptTitle
            ?: bem?.contentInfo?.detail?.title
        else -> target?.question?.title
            ?: target?.excerptTitle
            ?: target?.title
            ?: target?.name
            ?: bem?.parentContentData?.contentInfo?.detail?.title
            ?: bem?.contentInfo?.detail?.title
    } ?: ""

    val plainContent = when (momentType) {
        MomentContentType.USER -> target?.headline
        else -> target?.excerpt
            ?: target?.content?.firstOrNull { it.type == "text" }?.ownText
            ?: bem?.contentInfo?.detail?.plainContent
    } ?: ""

    val authorName = source?.actor?.name
        ?: bem?.author?.profile?.fullName
        ?: "Anonymous user"

    val authorAvatarUrl = source?.actor?.avatarUrl
        ?: bem?.author?.profile?.avatar?.url

    val routerUrl = target?.url
        ?: bem?.router

    val voteCount = target?.voteupCount
        ?: (bem?.reactionMap?.like?.count ?: 0) + (bem?.reactionMap?.voteUp?.count ?: 0)

    val commentCount = target?.commentCount
        ?: bem?.reactionMap?.comment?.count
        ?: 0

    val images = target?.legoInfo?.imageList?.map {
        MediaImage(url = it.originalUrl, width = it.width, height = it.height)
    } ?: bem?.contentInfo?.mediaDetail?.images ?: emptyList()

    val thumbnail = when (momentType) {
        MomentContentType.USER -> target?.avatarUrl
        MomentContentType.ARTICLE -> target?.imageUrl ?: images.firstOrNull()?.url
        else -> images.firstOrNull()?.url
    }

    val publishedAt = source?.actionTime
        ?: bem?.contentInfo?.publishedAt
        ?: 0

    val actionText = source?.actionText
        ?: bem?.momentsBizData?.actionText
        ?: ""

    val actionTime = source?.actionTime
        ?: (bem?.momentsBizData?.actionTimeMs ?: 0L) / 1000

    val collectCount = bem?.reactionMap?.collect?.count ?: 0

    val isTopping = bem?.momentsBizData?.feedType == "topping"

    return MomentDto(
        id = "${this.id}_${target?.id ?: bem?.contentInfo?.contentId ?: ""}",
        type = momentType,
        title = title,
        plainContent = plainContent,
        summary = target?.description ?: bem?.contentInfo?.detail?.summary ?: "",
        authorName = authorName,
        authorAvatarUrl = authorAvatarUrl,
        routerUrl = routerUrl,
        voteCount = voteCount,
        commentCount = commentCount,
        collectCount = collectCount,
        mediaImages = images,
        videoThumbnail = thumbnail,
        publishedAt = publishedAt,
        actionText = actionText,
        actionTime = actionTime,
        isTopping = isTopping,
    )
}


