package com.prslc.zhiflow.data.remote.parser

import com.prslc.zhiflow.data.remote.parser.model.CodeTokenKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tokenizer's contract with its renderer.
 *
 * The library answers with eight sets of ranges that overlap each other, and laying them down is
 * this file's job. What the renderer is promised in return is the shape asserted here: runs that
 * are in order, never overlap, and never cover text the tokenizer said nothing about. A regression
 * in any of those three draws colored text over the wrong characters, which is a thing no other
 * check in this repo would catch.
 */
class CodeHighlighterTest {

    private val kotlinCode = listOf(
        "// What a cart is worth, in cents.",
        "fun price(items: List<Int>): Int {",
        "    return if (items.size > 3) 12 else items.size",
        "}",
    ).joinToString("\n")

    @Test
    fun `runs are in order, never overlap, and stay inside the code`() {
        val code = kotlinCode
        val tokens = CodeHighlighter.highlight(code, "kotlin")

        assertTrue("a Kotlin block should come back with runs", tokens.isNotEmpty())

        var previousEnd = 0
        tokens.forEach { token ->
            assertTrue("a run starts before it ends: $token", token.start < token.end)
            assertTrue("a run starts before the last one ended: $token", token.start >= previousEnd)
            assertTrue("a run ends past the code: $token", token.end <= code.length)
            previousEnd = token.end
        }
    }

    @Test
    fun `a comment is one run, not the colors of what it contains`() {
        val code = "// foo.bar is not a member\nval x = 1"
        val tokens = CodeHighlighter.highlight(code, "kotlin")

        val overTheComment = tokens.filter { it.start < code.indexOf("member") }
        assertEquals("the comment should be a single run", 1, overTheComment.size)
        assertEquals(CodeTokenKind.Comment, overTheComment.single().kind)
        assertEquals(0, overTheComment.single().start)
        assertEquals(code.indexOf("\n"), overTheComment.single().end)
    }

    @Test
    fun `a fence naming a language the tokenizer does not carry yields no runs`() {
        val code = "SELECT id, title FROM segments"

        assertTrue(CodeHighlighter.highlight(code, "sql").isEmpty())
        assertTrue(CodeHighlighter.highlight(code, "text").isEmpty())
        assertTrue(CodeHighlighter.highlight(code, null).isEmpty())
        assertTrue(CodeHighlighter.highlight(code, "no-such-language").isEmpty())
    }

    @Test
    fun `an empty block yields no runs`() {
        assertTrue(CodeHighlighter.highlight("", "kotlin").isEmpty())
    }

    @Test
    fun `the names people type reach the languages they mean`() {
        // SyntaxLanguage knows only the enum's spelling, so these ride on the alias table; one
        // missing from it yields no runs at all, as an unknown language does.
        val fences = mapOf(
            "c++" to "int main() { return 0; }",
            "cxx" to "int main() { return 0; }",
            "kotlin" to "fun main() {}",
            "kt" to "fun main() {}",
            "js" to "function main() { return 0; }",
            "py" to "def main():\n    return 0",
        )

        fences.forEach { (fence, code) ->
            assertTrue("$fence should have resolved to a language", CodeHighlighter.highlight(code, fence).isNotEmpty())
        }
    }

    @Test
    fun `the fence is read the way a fence is written`() {
        val code = "fun main() {}"

        assertTrue(CodeHighlighter.highlight(code, "Kotlin").isNotEmpty())
        assertTrue(CodeHighlighter.highlight(code, " kotlin ").isNotEmpty())
    }
}
