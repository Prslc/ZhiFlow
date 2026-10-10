package com.prslc.zhiflow.data.remote.parser

/**
 * The address a card leading off Zhihu points at, settled from the fields that all claim to carry
 * it.
 *
 * Such a card repeats its address: its own url, `extra_info`'s url and desc, and the title, where a
 * share text keeps the line that was pasted. The editor those early cards were written in took the
 * whole pasted line for a host, so any of those fields can hold an address no resolver will open --
 * the punycode label it produced runs past the DNS limit and keeps the `%` of an escaped space. The
 * address the card meant to share is still inside that string, behind the run and its `//`, and in
 * the title it is there whole. Each field is read for the last http(s) address in it whose host can
 * stand, and whatever precedes that address is dropped.
 *
 * A field that holds an address and nothing else is read as one; a title is read as prose, where
 * the address ends as soon as the words resume. Only a card off Zhihu goes through here -- a card
 * to our own content opens by content id, and its url is read by [LinkParser] as it always was.
 */
internal object CardLink {

    /** A DNS label is ASCII letters, digits and inner hyphens, 63 bytes at the most. */
    private const val LABEL_MAX_LENGTH = 63

    /** The last printable ASCII character; anything past it ends an address. */
    private const val ASCII_MAX = 0x7E

    private val SCHEME = Regex("https?://")

    /**
     * The first field holding an address which can be opened.
     *
     * @param addresses The fields that are an address and nothing else, most trustworthy first.
     * @param prose The fields that are text which may carry an address, read after them.
     * @return The address, or null when no field holds one that resolves.
     */
    fun resolve(addresses: List<String?>, prose: List<String?>): String? =
        firstAddressIn(addresses, isProse = false) ?: firstAddressIn(prose, isProse = true)

    private fun firstAddressIn(fields: List<String?>, isProse: Boolean): String? =
        fields.firstNotNullOfOrNull { field ->
            field?.takeIf { it.isNotBlank() }?.let { addressIn(it, isProse) }
        }

    /**
     * The last address in [source] whose host can stand, the redirect shell around the string and
     * the text in front of that address dropped.
     *
     * @param isProse Whether [source] is text rather than a field holding an address.
     */
    private fun addressIn(source: String, isProse: Boolean): String? {
        val text = LinkParser.unwrapRedirect(source)
        return SCHEME.findAll(text)
            .mapNotNull { candidateAt(text, it.range.first, isProse) }
            .lastOrNull()
    }

    /**
     * The address starting at [start], cut where the text stops looking like one -- a space, or the
     * first character no URL holds -- or null when what is left is no address.
     *
     * @param isProse Whether the field is prose. Prose goes on after the address, so the cut there
     *   is where it ends; an address field that runs into characters no address holds is one we
     *   cannot read, and its first half would be an address we invented.
     */
    private fun candidateAt(text: String, start: Int, isProse: Boolean): String? {
        val tail = text.substring(start)
        val end = tail.indexOfFirst { c -> c.isWhitespace() || c.code > ASCII_MAX }
        val candidate = if (end == -1) tail else tail.substring(0, end)
        if (!isProse && end != -1 && tail[end].code > ASCII_MAX) return null

        // A candidate still wearing the shell is not an address: the shell is what a link out is
        // wrapped in, not where it goes.
        return candidate.takeIf { !LinkParser.isRedirect(it) && hostCanStand(it) }
    }

    /**
     * Whether the host of [url] is one a name can be made of: labels of letters, digits and inner
     * hyphens, at least a name and a top-level one. What a pasted line becomes when it is taken for
     * a host fails this.
     */
    private fun hostCanStand(url: String): Boolean {
        val labels = hostOf(url).split('.')
        return labels.size > 1 && labels.all { label ->
            label.isNotEmpty() && label.length <= LABEL_MAX_LENGTH && label.all(::isLabelChar)
        }
    }

    private fun hostOf(url: String): String = url.substringAfter("://", "")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('@')
        .substringBefore(':')

    private fun isLabelChar(c: Char) =
        c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '-'
}
