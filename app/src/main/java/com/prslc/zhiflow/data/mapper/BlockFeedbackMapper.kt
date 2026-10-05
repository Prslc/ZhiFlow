package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.BlockOptions
import com.prslc.zhiflow.data.dto.BlockTag
import com.prslc.zhiflow.data.model.feedback.ContentTag
import com.prslc.zhiflow.data.model.feedback.FeedRootBlock

internal fun ContentTag.toDto(): BlockTag = BlockTag(id = id, label = value)

internal fun FeedRootBlock.toDto(tags: List<BlockTag>): BlockOptions = BlockOptions(
    tags = tags,
    keywords = data,
    keywordMinLength = minLength,
    keywordMaxLength = maxLength,
    keywordMaxCount = maxCount,
)
