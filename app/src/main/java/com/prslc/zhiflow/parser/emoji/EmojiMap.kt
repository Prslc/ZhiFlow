package com.prslc.zhiflow.parser.emoji

object EmojiMap {
    private const val SCHEME = "file:///android_asset/"
    const val ASSET_PREFIX = "emoji/default/"

    fun getFullUrl(fileName: String): String {
        return "$SCHEME$ASSET_PREFIX$fileName"
    }

    val tagToFileName = mapOf(
        "[谢邀]" to "emoji_1.webp",
        "[赞同]" to "emoji_2.webp",
        "[爱]" to "emoji_3.webp",
        "[害羞]" to "emoji_4.webp",
        "[好奇]" to "emoji_5.webp",
        "[思考]" to "emoji_6.webp",
        "[酷]" to "emoji_7.webp",
        "[大笑]" to "emoji_8.webp",
        "[微笑]" to "emoji_9.webp",
        "[捂脸]" to "emoji_10.webp",
        "[捂嘴]" to "emoji_11.webp",
        "[飙泪笑]" to "emoji_12.webp",
        "[耶]" to "emoji_13.webp",
        "[可怜]" to "emoji_14.webp",
        "[惊喜]" to "emoji_15.webp",
        "[流泪]" to "emoji_16.webp",
        "[大哭]" to "emoji_17.webp",
        "[生气]" to "emoji_18.webp",
        "[惊讶]" to "emoji_19.webp",
        "[调皮]" to "emoji_20.webp",
        "[衰]" to "emoji_21.webp",
        "[发呆]" to "emoji_22.webp",
        "[机智]" to "emoji_23.webp",
        "[嘘]" to "emoji_24.webp",
        "[尴尬]" to "emoji_25.webp",
        "[小情绪]" to "emoji_26.webp",
        "[为难]" to "emoji_27.webp",
        "[吃瓜]" to "emoji_28.webp",
        "[语塞]" to "emoji_29.webp",
        "[看看你]" to "emoji_30.webp",
        "[撇嘴]" to "emoji_31.webp",
        "[魔性笑]" to "emoji_32.webp",
        "[潜水]" to "emoji_33.webp",
        "[口罩]" to "emoji_34.webp",
        "[开心]" to "emoji_35.webp",
        "[滑稽]" to "emoji_36.webp",
        "[笑哭]" to "emoji_37.webp",
        "[白眼]" to "emoji_38.webp",
        "[红心]" to "emoji_39.webp",
        "[柠檬]" to "emoji_40.webp",
        "[拜托]" to "emoji_41.webp",
        "[握手]" to "emoji_42.webp",
        "[赞]" to "emoji_43.webp",
        "[发火]" to "emoji_44.webp",
        "[不抬杠]" to "emoji_45.webp",
        "[种草]" to "emoji_46.webp",
        "[抱抱]" to "emoji_47.webp",
        "[doge]" to "emoji_48.webp",
        "[蹲]" to "emoji_49.webp",
        "[知乎益蜂]" to "emoji_50.webp",
        "[百分百赞]" to "emoji_51.webp",
        "[为爱发乎]" to "emoji_52.webp",
        "[脑爆]" to "emoji_53.webp",
        "[暗中学习]" to "emoji_54.webp",
        "[匿了]" to "emoji_55.webp",
        "[感谢]" to "emoji_56.webp",
        "[哇]" to "emoji_57.webp",
        "[打招呼]" to "emoji_58.webp"
    )
}