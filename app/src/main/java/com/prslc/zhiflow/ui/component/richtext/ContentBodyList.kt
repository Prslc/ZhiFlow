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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import coil3.compose.AsyncImage
import com.prslc.zhiflow.R
import com.prslc.zhiflow.core.utils.compose.ReadingPosition
import com.prslc.zhiflow.core.utils.compose.ReadingProgressEffect
import com.prslc.zhiflow.data.model.content.AnswerAuthor
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.data.model.content.ZhihuContent
import com.prslc.zhiflow.data.model.content.ZhihuImage
import com.prslc.zhiflow.data.remote.parser.model.InlineFormulaMeta
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.ui.component.common.FollowButton
import com.prslc.zhiflow.ui.component.widget.ImageLightboxController
import com.prslc.zhiflow.ui.navigation.Navigator
import org.koin.compose.koinInject

/** The items [ContentBodyList] lays out before the body: the author's row. */
private const val LEADING_ITEMS = 1

/**
 * One heading, as the outline lists it: [depth] is 0 for the shallowest heading in the body, and the
 * content is carried whole — inline formulas included — so the outline can draw what the body draws.
 */
@Immutable
data class OutlineEntry(
    val content: AnnotatedString,
    val inlineMetas: List<InlineFormulaMeta>,
    val depth: Int,
    val index: Int,
)

/**
 * The state of a content body list: the list, plus the geometry anything outside it needs to reason
 * about — which is where the progress bar and the outline read from.
 *
 * [bodyEnd] is the index of the first item after the body. Trailing chrome stays out of the scale, so
 * the progress bar completes at the end of the text rather than at the end of the list.
 */
@Stable
class ContentBodyState internal constructor(
    val listState: LazyListState,
    val bodyEnd: Int,
    val outline: List<OutlineEntry>,
) {
    /** Which outline entry the reader is in, or null while they are still above the first one. */
    val currentOutlineIndex: Int? by derivedStateOf {
        outline.indexOfLast { it.index <= listState.firstVisibleItemIndex }.takeIf { it >= 0 }
    }

    /** Scrolls the body so [entry] ends up at the top of the part the reader can see. */
    suspend fun scrollTo(entry: OutlineEntry) {
        listState.animateScrollToItem(entry.index)
    }
}

@Composable
fun rememberContentBodyState(elements: List<RichTextElement>): ContentBodyState {
    val listState = rememberLazyListState()
    return remember(listState, elements) {
        ContentBodyState(
            listState = listState,
            bodyEnd = LEADING_ITEMS + elements.size,
            outline = outlineOf(elements),
        )
    }
}

/**
 * The headings of [elements], indented by how far each sits below the shallowest one: a writer's own
 * levels can start anywhere, so they mean something only relative to each other.
 */
private fun outlineOf(elements: List<RichTextElement>): List<OutlineEntry> {
    val headings = elements.mapIndexedNotNull { index, element ->
        val heading = element as? RichTextElement.Heading ?: return@mapIndexedNotNull null
        // A heading that is nothing but a formula is still a heading: its text is the placeholder the
        // formula is drawn over, never blank.
        if (heading.content.text.isBlank()) null else index to heading
    }
    val shallowest = headings.minOfOrNull { (_, heading) -> heading.level } ?: return emptyList()

    return headings.map { (index, heading) ->
        OutlineEntry(
            content = heading.content,
            inlineMetas = heading.inlineMetas,
            depth = heading.level - shallowest,
            index = LEADING_ITEMS + index,
        )
    }
}

/**
 * The body of a piece of content: the author's row, what they wrote, and the line saying when. Both
 * detail screens render the same list, so it and the geometry of it are kept in one place.
 *
 * @param topPadding a producer rather than a value: it is read while measuring, so a frame in which
 *   the bar above has just changed height places the list against that height rather than the one
 *   before it.
 */
@Composable
fun ContentBodyList(
    content: ZhihuContent,
    elements: List<RichTextElement>,
    state: ContentBodyState,
    navigator: Navigator,
    topPadding: () -> Dp,
    bodyComplete: Boolean,
    showAuthorDivider: Boolean,
    onFollowClick: () -> Unit,
    modifier: Modifier = Modifier,
    segmentLikes: Map<String, SegmentLikeTarget> = emptyMap(),
    onSegmentLikeClick: (String) -> Unit = {},
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
                .topPadding(topPadding),
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
                        segmentLikes = segmentLikes,
                        onSegmentLikeClick = onSegmentLikeClick,
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

/**
 * Top padding resolved while measuring, where [Modifier.padding] would have resolved it while
 * composing. `Scaffold` hands its content a `PaddingValues` backed by a state it writes once the top
 * bar has been measured — after that frame's composition has already run — so a value read while
 * composing describes the previous frame, and a list under a bar that has just changed height is
 * placed against the height the bar no longer has. Read here, it belongs to the frame it measures in.
 */
private fun Modifier.topPadding(top: () -> Dp): Modifier = layout { measurable, constraints ->
    val topPx = top().roundToPx()
    val placeable = measurable.measure(constraints.offset(vertical = -topPx))
    layout(placeable.width, constraints.constrainHeight(placeable.height + topPx)) {
        placeable.placeRelative(0, topPx)
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
