package com.prslc.zhiflow.data.remote.parser

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.remote.parser.model.CodeToken
import com.prslc.zhiflow.data.remote.parser.model.CodeTokenKind
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.CodeStructure
import dev.snipme.highlights.model.PhraseLocation
import dev.snipme.highlights.model.SyntaxLanguage

@Immutable
object CodeHighlighter {

    /**
     * The fence is written by hand, so the names people type come alongside the enum spelling:
     * `SyntaxLanguage.getByName` knows only the latter and would miss `c++`, `js`, `bash`.
     */
    private val LANGUAGES: Map<String, SyntaxLanguage> = buildMap {
        SyntaxLanguage.entries.forEach { language ->
            if (language != SyntaxLanguage.DEFAULT) put(language.name.lowercase(), language)
        }
        put("c++", SyntaxLanguage.CPP)
        put("cplusplus", SyntaxLanguage.CPP)
        put("cxx", SyntaxLanguage.CPP)
        put("cs", SyntaxLanguage.CSHARP)
        put("c#", SyntaxLanguage.CSHARP)
        put("coffee", SyntaxLanguage.COFFEESCRIPT)
        put("js", SyntaxLanguage.JAVASCRIPT)
        put("jsx", SyntaxLanguage.JAVASCRIPT)
        put("node", SyntaxLanguage.JAVASCRIPT)
        put("kt", SyntaxLanguage.KOTLIN)
        put("kts", SyntaxLanguage.KOTLIN)
        put("pl", SyntaxLanguage.PERL)
        put("py", SyntaxLanguage.PYTHON)
        put("python3", SyntaxLanguage.PYTHON)
        put("rb", SyntaxLanguage.RUBY)
        put("rs", SyntaxLanguage.RUST)
        put("sh", SyntaxLanguage.SHELL)
        put("bash", SyntaxLanguage.SHELL)
        put("zsh", SyntaxLanguage.SHELL)
        put("console", SyntaxLanguage.SHELL)
        put("ts", SyntaxLanguage.TYPESCRIPT)
        put("tsx", SyntaxLanguage.TYPESCRIPT)
        put("golang", SyntaxLanguage.GO)
    }

    /**
     * Tokenize one code block into the runs its renderer paints.
     *
     * An empty list means "draw it in a single colour", which is what comes back for an empty
     * block, for a fence naming no language we know (`text`, and the languages the tokenizer does
     * not carry, both land here), and for a tokenizer that failed. Nothing tells those apart: the
     * renderer has the whole code either way, and the fence's own string still reaches it.
     */
    fun highlight(code: String, language: String?): List<CodeToken> {
        if (code.isEmpty()) return emptyList()
        val syntax = LANGUAGES[language?.trim()?.lowercase()] ?: return emptyList()

        // A fresh instance per block, against the library's advice to keep one around: it holds the
        // analysed code in mutable state, so a shared one would answer with whatever block another
        // page's parse had set a moment earlier.
        val structure = try {
            Highlights.Builder().code(code).language(syntax).build().getCodeStructure()
        } catch (e: Exception) {
            // A tokenizer bug costs this block its colours, not the article its body. The call
            // cannot suspend, so there is no cancellation in flight here to swallow.
            return emptyList()
        }

        return resolve(code, structure)
    }

    /**
     * Paint the eight sets over the code, then cut what is left into runs.
     *
     * The sets overlap by design: only comments and strings are excluded from the keyword search,
     * so the `.` in `// foo.bar` is punctuation and comment at once. The library settles that by
     * adding them in one fixed order, replayed here so the later set wins — that order is what
     * keeps a comment from being speckled with the colours of what it contains.
     */
    private fun resolve(code: String, structure: CodeStructure): List<CodeToken> {
        val kinds = arrayOfNulls<CodeTokenKind>(code.length)

        paint(kinds, structure.marks, CodeTokenKind.Mark)
        paint(kinds, structure.punctuations, CodeTokenKind.Punctuation)
        paint(kinds, structure.keywords, CodeTokenKind.Keyword)
        paint(kinds, structure.strings, CodeTokenKind.String)
        paint(kinds, structure.literals, CodeTokenKind.Literal)
        paint(kinds, structure.annotations, CodeTokenKind.Annotation)
        paint(kinds, structure.comments, CodeTokenKind.Comment)
        paint(kinds, structure.multilineComments, CodeTokenKind.MultilineComment)

        val tokens = mutableListOf<CodeToken>()
        var start = 0
        for (i in 1..code.length) {
            if (i < code.length && kinds[i] == kinds[start]) continue
            kinds[start]?.let { tokens += CodeToken(start, i, it) }
            start = i
        }
        return tokens
    }

    /**
     * Lay one set's ranges over the runs an earlier set may already have claimed.
     *
     * The ranges are clamped rather than trusted: they come out of a third-party tokenizer, and an
     * end past the text would throw here, in a parse that has nothing left to say about it.
     */
    private fun paint(
        kinds: Array<CodeTokenKind?>,
        locations: Set<PhraseLocation>,
        kind: CodeTokenKind,
    ) {
        locations.forEach { location ->
            val from = location.start.coerceIn(0, kinds.size)
            val to = location.end.coerceIn(from, kinds.size)
            for (i in from until to) kinds[i] = kind
        }
    }
}
