package com.prslc.zhiflow.ui.component.richtext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.utils.compose.ReadingPosition
import com.prslc.zhiflow.core.utils.compose.ReadingProgressEffect
import com.prslc.zhiflow.data.model.content.AnswerAuthor
import com.prslc.zhiflow.data.model.content.ZhihuContent
import com.prslc.zhiflow.data.model.content.ZhihuImage
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.ui.component.common.FollowButton
import com.prslc.zhiflow.ui.component.widget.ImageLightboxController
import com.prslc.zhiflow.ui.navigation.Navigator
import org.koin.compose.koinInject

/** The items [ContentBodyList] lays out before the body: the author's row. */
private const val LEADING_ITEMS = 1

/**
 * The state of a content body list: the list, plus the geometry anything outside it needs to reason
 * about — which is where the progress bar and, later, the outline read from.
 *
 * [bodyEnd] is the index of the first item after the body. Trailing chrome stays out of the scale, so
 * the progress bar completes at the end of the text rather than at the end of the list.
 */
@Stable
class ContentBodyState internal constructor(
    val listState: LazyListState,
    val bodyEnd: Int,
)

@Composable
fun rememberContentBodyState(elements: List<RichTextElement>): ContentBodyState {
    val listState = rememberLazyListState()
    return remember(listState, elements) {
        ContentBodyState(listState, LEADING_ITEMS + elements.size)
    }
}

/**
 * The body of a piece of content: the author's row, what they wrote, and the line saying when. Both
 * detail screens render the same list, so it and the geometry of it are kept in one place.
 */
@Composable
fun ContentBodyList(
    content: ZhihuContent,
    elements: List<RichTextElement>,
    state: ContentBodyState,
    navigator: Navigator,
    topPadding: Dp,
    bodyComplete: Boolean,
    showAuthorDivider: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
    onProgress: (ReadingPosition) -> Unit,
) {
    val lightbox = koinInject<ImageLightboxController>()
    val images = remember(elements) {
        elements.filterIsInstance<RichTextElement.Image>().map { it.data }
    }
    val onImageClick: (ZhihuImage) -> Unit = { tapped -> lightbox.open(images, tapped) }

    ReadingProgressEffect(
        state = state.listState,
        bodyEnd = state.bodyEnd,
        bodyComplete = bodyComplete,
        onProgress = onProgress,
    )

    // Wraps the whole list on purpose: moving this inside the items loop would cap selection at a
    // single paragraph. Compose pins selected lazy items, so recycling does not drop the selection.
    SelectionContainer(modifier = modifier) {
        LazyColumn(
            state = state.listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                DisableSelection {
                    AuthorSection(
                        author = content.author,
                        navigator = navigator,
                        onFollowClick = onFollowClick
                    )
                    if (showAuthorDivider) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }

            itemsIndexed(
                items = elements,
                key = { index, element ->
                    when (element) {
                        is RichTextElement.Divider -> "divider_$index"
                        is RichTextElement.Image -> "img_${element.data.urls.firstOrNull()}_$index"
                        else -> "content_${element::class.simpleName}_$index"
                    }
                },
                contentType = { _, element -> element::class.simpleName }
            ) { _, element ->
                // List items are consecutive lines, not paragraphs: paragraph spacing leaves every
                // bullet floating on its own. The row carries its own 2dp.
                val vertical = if (element is RichTextElement.BulletItem) 0.dp else 16.dp
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = vertical)
                ) {
                    RichTextSingleElement(
                        element = element,
                        onImageClick = onImageClick,
                    )
                }
            }

            item {
                DisableSelection {
                    content.contentEnd?.let { contentEnd ->
                        val timeDisplay = contentEnd.updateTime?.takeIf { it.isNotBlank() }
                            ?: contentEnd.createTime?.takeIf { it.isNotBlank() }

                        if (!timeDisplay.isNullOrBlank()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                val text = if (contentEnd.ipInfo.isNotEmpty()) {
                                    stringResource(
                                        R.string.content_published_with_ip,
                                        contentEnd.ipInfo,
                                        timeDisplay
                                    )
                                } else {
                                    stringResource(
                                        R.string.content_published_no_ip,
                                        timeDisplay
                                    )
                                }

                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthorSection(
    author: AnswerAuthor,
    navigator: Navigator,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        AsyncImage(
            model = author.avatar?.avatarImage?.day,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { navigator.navigateToPeople(author.urlToken) }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = author.fullname,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (author.description.isNotEmpty()) {
                Text(
                    text = author.description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        FollowButton(
            isFollowing = author.followStatus == "following",
            onClick = onFollowClick,
            compact = true
        )
    }
}
