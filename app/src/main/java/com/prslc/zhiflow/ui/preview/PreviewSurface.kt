package com.prslc.zhiflow.ui.preview

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavHostController
import com.prslc.zhiflow.ui.component.widget.ImageLightboxController
import com.prslc.zhiflow.ui.navigation.LocalNavigator
import com.prslc.zhiflow.ui.navigation.Navigator
import com.prslc.zhiflow.ui.theme.ZhiFlowTheme
import org.koin.compose.KoinApplicationPreview
import org.koin.dsl.module

/**
 * What a previewed widget is allowed to ask Koin for.
 *
 * A widget that opens the lightbox resolves the controller itself -- `koinInject`, not a parameter,
 * since every screen would otherwise have to carry one. Nothing here starts Koin: a preview has no
 * Application, and [KoinApplicationPreview] builds an isolated instance for the composition rather
 * than touching the global one.
 */
private val previewModule = module {
    single { ImageLightboxController() }
}

/**
 * What every preview in this app is drawn inside.
 *
 * Four things a bare `@Preview` would be missing:
 *
 * - The theme, with `dynamicColor` off. A preview has no wallpaper to read a palette from, and the
 *   app's own scheme is the one worth looking at anyway: [ZhiFlowTheme] with no source is a
 *   different set of colors from either mode the app actually runs in.
 * - A `LocalNavigator`. It has no default -- reading it without a provider is an error, by design --
 *   and a good many components resolve their own taps through it, so a preview of one would throw
 *   rather than draw. The controller it wraps is never navigated with; a tap in a preview can do
 *   nothing.
 * - Koin, for the widgets that resolve the lightbox themselves.
 * - A background, at the full width, since most of what is previewed is a row meant to be drawn
 *   edge to edge.
 *
 * @param modifier Applied inside the full width every preview is given.
 * @param darkTheme Which mode to draw in. It reads the preview's own `uiMode`, which is what makes
 *   [AppPreviews]' two annotations draw the same component in both modes.
 * @param content The component being previewed.
 */
@Composable
fun PreviewSurface(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val navigator = remember(context) {
        Navigator(
            navController = NavHostController(context),
            context = context,
            uriHandler = object : UriHandler {
                override fun openUri(uri: String) = Unit
            },
        )
    }

    KoinApplicationPreview(application = { modules(previewModule) }) {
        ZhiFlowTheme(darkTheme = darkTheme, dynamicColor = false) {
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(modifier),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    content()
                }
            }
        }
    }
}
