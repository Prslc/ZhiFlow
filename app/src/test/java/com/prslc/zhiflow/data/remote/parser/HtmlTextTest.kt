package com.prslc.zhiflow.data.remote.parser

import android.app.Application
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.prslc.zhiflow.data.remote.parser.engine.AnnotatedStringBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The backend's HTML as this app draws it.
 *
 * These run under Robolectric because the work is `android.text.Html`'s: the spans it produces are
 * the ground every offset here is measured on, and standing in for it would be testing the stand-in.
 *
 * The one worth keeping honest is the `<em>`: the search endpoint wraps a matched word in it and
 * the documentation says the markup lives in a `highlight` layer of its own, which it does not --
 * the title, the excerpt and the author's name all arrive with it inline. A field read as plain
 * text draws an italic nobody asked for, and an author's name is where that is least wanted.
 *
 * The plain [Application] is deliberate. Robolectric otherwise builds the app's own, whose
 * `onCreate` starts Koin and goes out for a session: a test of a text helper has no business doing
 * either, and the second test in the class would find Koin already started and fail on it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HtmlTextTest {

    // Plain text

    @Test
    fun `a marked word comes off a plain field with the rest of the markup`() {
        assertEquals("How parsers work", htmlToPlainText("How <em>parsers</em> work"))
    }

    @Test
    fun `entities are decoded`() {
        assertEquals("a & b", htmlToPlainText("a &amp; b"))
        assertEquals("<not a tag>", htmlToPlainText("&lt;not a tag&gt;"))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals("parsers", htmlToPlainText("\n  parsers  \n"))
    }

    @Test
    fun `a search link keeps its words and loses its address`() {
        val html = """see <a class="search_word" href="https://www.zhihu.com/search?q=parsers">parsers</a> here"""

        assertEquals("see parsers here", htmlToPlainText(html))
    }

    // Marked text

    @Test
    fun `the words the query matched are carried as a weight, not as an italic`() {
        val marked = searchTextToAnnotatedString("How <em>parsers</em> work")

        assertEquals("How parsers work", marked.text)

        val bold = marked.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, bold.size)
        assertEquals("parsers", marked.text.substring(bold.single().start, bold.single().end))
    }

    @Test
    fun `every marked word in a field is carried, not only the first`() {
        val marked = searchTextToAnnotatedString("<em>parsers</em> and <em>segments</em>")

        val bold = marked.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(
            listOf("parsers", "segments"),
            bold.map { marked.text.substring(it.start, it.end) },
        )
    }

    @Test
    fun `a field with no markup is left as the text it is`() {
        val marked = searchTextToAnnotatedString("How parsers work")

        assertEquals("How parsers work", marked.text)
        assertTrue(marked.spanStyles.isEmpty())
    }

    // Links

    @Test
    fun `a link the backend means is kept to be tapped`() {
        val marked = htmlToAnnotatedString("""see <a href="https://example.test/x">this</a>""")

        assertEquals("see this", marked.text)
        val links = marked.getStringAnnotations(AnnotatedStringBuilder.URL_TAG, 0, marked.length)
        assertEquals(1, links.size)
        assertEquals("https://example.test/x", links.single().item)
    }

    @Test
    fun `a link to a search is not kept, because this app does not run one`() {
        // The word belongs to the sentence and stays; the address wrapped around it does not.
        val html = """see <a class="search_word" href="https://www.zhihu.com/search?q=parsers">parsers</a>"""

        val marked = htmlToAnnotatedString(html)

        assertEquals("see parsers", marked.text)
        assertTrue(marked.getStringAnnotations(AnnotatedStringBuilder.URL_TAG, 0, marked.length).isEmpty())
    }

    @Test
    fun `leading whitespace does not shift where a link points`() {
        // The spans are measured over the untrimmed text, so every offset has to be moved by what
        // the trim took off the front.
        val marked: AnnotatedString = htmlToAnnotatedString("""  <a href="https://example.test/x">this</a>""")

        assertEquals("this", marked.text)
        val links = marked.getStringAnnotations(AnnotatedStringBuilder.URL_TAG, 0, marked.length)
        assertEquals(1, links.size)
        assertEquals(0, links.single().start)
        assertEquals(4, links.single().end)
    }
}
