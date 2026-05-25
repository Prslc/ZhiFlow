package com.prslc.zhiflow.ui.page.content

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
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
import com.prslc.zhiflow.data.model.AnswerAuthor
import com.prslc.zhiflow.data.model.ZhihuContent
import com.prslc.zhiflow.parser.model.RichTextElement
import com.prslc.zhiflow.ui.component.richtext.RichTextSingleElement
import com.prslc.zhiflow.ui.navigation.Navigator

@Composable
fun ContentRichTextList(
    id: String,
    richTextElements: List<RichTextElement>,
    answer: ZhihuContent,
    navigator: Navigator,
    topPadding: Dp,
    onImageClick: (String) -> Unit,
    onProgress: (Int) -> Unit
) {
    val lazyListState = rememberLazyListState()

    LaunchedEffect(id) {
        snapshotFlow {
            val layout = lazyListState.layoutInfo
            val total = layout.totalItemsCount
            if (total <= 0) 0
            else {
                val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
                ((lastVisible + 1).toFloat() / total * 100).toInt().coerceIn(0, 100)
            }
        }.collect { progress ->
            onProgress(progress)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            AuthorSection(
                author = answer.author,
                navigator = navigator
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }

        itemsIndexed(
            items = richTextElements,
            key = { index, element ->
                when (element) {
                    is RichTextElement.Divider -> "divider_$index"
                    is RichTextElement.Image -> "img_${element.data.urls.firstOrNull()}_$index"
                    else -> "${element.hashCode()}_$index"
                }
            },
            contentType = { _, element -> element::class.simpleName }
        ) { _, element ->
            Box(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                RichTextSingleElement(
                    element = element,
                    onImageClick = onImageClick
                )
            }
        }

        item {
            answer.contentEnd?.let { contentEnd ->
                val timeDisplay = contentEnd.updateTime?.takeIf { it.isNotBlank() }
                    ?: contentEnd.createTime?.takeIf { it.isNotBlank() }

                if (!timeDisplay.isNullOrBlank()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        val text = if (contentEnd.ipInfo.isNotEmpty()) {
                            stringResource(
                                R.string.answer_published_with_ip,
                                contentEnd.ipInfo,
                                timeDisplay
                            )
                        } else {
                            stringResource(
                                R.string.answer_published_no_ip,
                                timeDisplay
                            )
                        }

                        Text(
                            text = text,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorSection(
    author: AnswerAuthor,
    navigator: Navigator
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
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
                fontWeight = FontWeight.Bold
            )
            if (author.description.isNotEmpty()) {
                Text(
                    text = author.description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
