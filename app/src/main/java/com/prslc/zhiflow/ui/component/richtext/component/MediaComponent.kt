package com.prslc.zhiflow.ui.component.richtext.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.prslc.zhiflow.R
import com.prslc.zhiflow.data.model.content.ZhihuImage
import com.prslc.zhiflow.data.remote.parser.model.RichTextElement
import com.prslc.zhiflow.ui.component.LocalDataSaver
import com.prslc.zhiflow.ui.component.common.ImageOmitted
import com.prslc.zhiflow.ui.component.richtext.bodyElementPadding
import com.prslc.zhiflow.ui.preview.AppPreviews
import com.prslc.zhiflow.ui.preview.PreviewSurface
import com.prslc.zhiflow.ui.preview.sampleZhihuImage
import com.prslc.zhiflow.ui.theme.TextStyles

/**
 * How long a QR mask takes to give way, in milliseconds. Shorter than a screen transition, because
 * nothing travels: the two occupy the same box and one is replacing the other.
 */
private const val REVEAL_FADE_MS = 200

/**
 * An image in a body, with its caption under it.
 *
 * @param image The image to draw, or null to draw nothing. One the API gave no display url for
 *   draws nothing either.
 * @param onImageClick Called with the image that was tapped.
 * @param modifier Applied to the column that holds the image and its caption, ahead of the
 *   `fillMaxWidth` it adds itself.
 */
@Composable
fun ImageComponent(
    image: ZhihuImage?,
    onImageClick: (ZhihuImage) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (image == null) return
    val displayUrl = image.displayUrl ?: return

    val aspectRatio = remember(image) {
        if (image.width > 0 && image.height > 0) {
            image.width.toFloat() / image.height.toFloat()
        } else {
            1.77f // 16:9
        }
    }

    // A picture the server found a code in is revealed rather than opened: the first tap is what
    // says the reader means to look at it, and only the second one opens it like any other.
    var revealed by remember(image) { mutableStateOf(false) }
    val masked = image.hasQrCode && !revealed

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clickable { if (masked) revealed = true else onImageClick(image) },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        ) {
            // Faded rather than cut, and timed by nothing: the mask gives way when the picture is
            // ready, so the wait for it is the network's and not this.
            Crossfade(targetState = masked, animationSpec = tween(REVEAL_FADE_MS)) { isMasked ->
                when {
                    isMasked -> QrMask(image.thumbnailUrl)

                    !LocalDataSaver.current -> AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(displayUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth,
                    )

                    else -> ImageOmitted()
                }
            }
        }
        if (image.description.isNotBlank()) {
            // Deliberately out of selection; the caption sits hard against the tappable image.
            DisableSelection {
                Text(
                    text = image.description,
                    modifier = Modifier
                        .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = TextStyles.imageCaptionSize,
                        lineHeight = 18.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * What stands in front of a picture the server found a QR code in, until the reader asks for it.
 *
 * The wording says what the picture is and what a tap does, and passes no judgement: this client
 * hides nothing, it hands the decision to the reader.
 *
 * Behind the words is the smaller of the two copies the API sent, blurred, under a scrim that
 * keeps the words legible over whatever the picture happens to be. Small, because a mask must not
 * cost what the picture would: the pair measured 4 KB against 56 KB. Blurred, because the smaller
 * copy alone is not enough -- at a hundred pixels a code's modules come out about five pixels
 * across, which a camera can still read. The full-size copy is never fetched here, so a picture
 * that arrived without a second one is covered by the flat tone alone rather than by its own
 * weight.
 *
 * @param thumbnailUrl The smaller copy to blur, or null when the card sent only one address.
 */
@Composable
private fun QrMask(thumbnailUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(24.dp, BlurredEdgeTreatment.Rectangle),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            )
        }
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.richtext_qr_masked),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.richtext_qr_reveal),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The body's own illustration, which is one of the two things the data saver reaches. */
@AppPreviews
@Composable
private fun ImageComponentPreview() {
    val element = RichTextElement.Image(sampleZhihuImage)
    PreviewSurface {
        ImageComponent(
            image = sampleZhihuImage,
            onImageClick = {},
            modifier = Modifier.padding(bodyElementPadding(element)),
        )
    }
}

/**
 * The same picture, marked as one the server found a code in.
 *
 * A preview fetches nothing, so the blurred copy behind the words is absent and what shows is the
 * flat tone the words sit on -- which is also what a reader sees for the moment before that copy
 * lands. Neither half of the mask can be judged here: what the blur looks like needs a device and a
 * card that has one, and what the tap does is one `remember` needing the same.
 */
@AppPreviews
@Composable
private fun QrMaskedImagePreview() {
    val masked = sampleZhihuImage.copy(hasQrCode = true)
    val element = RichTextElement.Image(masked)
    PreviewSurface {
        ImageComponent(
            image = masked,
            onImageClick = {},
            modifier = Modifier.padding(bodyElementPadding(element)),
        )
    }
}
