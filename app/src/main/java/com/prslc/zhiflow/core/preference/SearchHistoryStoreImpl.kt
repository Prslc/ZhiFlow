package com.prslc.zhiflow.core.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * [SearchHistoryStore] over [SharedPreferences], the same file the settings and the session use.
 *
 * The list is stored as a JSON array rather than as a joined string, so a phrase carrying whatever
 * character a separator might use comes back as it was written.
 *
 * @param preferences Where the list is kept; Koin supplies the app's own file.
 */
class SearchHistoryStoreImpl(private val preferences: SharedPreferences) : SearchHistoryStore {

    private val _queries = MutableStateFlow(readQueries())

    override val queries: StateFlow<List<String>> = _queries.asStateFlow()

    override fun record(query: String) {
        val phrase = query.trim()
        if (phrase.isEmpty()) return

        val current = _queries.value
        val next = (listOf(phrase) + current.filterNot { it == phrase }).take(MAX_QUERIES)
        if (next == current) return

        store(next)
    }

    override fun remove(query: String) {
        val current = _queries.value
        if (query !in current) return

        store(current.filterNot { it == query })
    }

    override fun clear() {
        if (_queries.value.isEmpty()) return

        store(emptyList())
    }

    /** Publishes [queries] and writes it, dropping the key rather than storing an empty list. */
    private fun store(queries: List<String>) {
        _queries.value = queries
        if (queries.isEmpty()) {
            preferences.edit { remove(KEY_QUERIES) }
        } else {
            preferences.edit { putString(KEY_QUERIES, Json.encodeToString(queries)) }
        }
    }

    /**
     * @return The stored phrases, or none when nothing readable is stored. A value that cannot be
     *   read is treated as no history rather than as a failure: it is a record of what was typed,
     *   and the box draws without it.
     */
    private fun readQueries(): List<String> = preferences.getString(KEY_QUERIES, null)
        ?.let { stored ->
            try {
                Json.decodeFromString<List<String>>(stored)
            } catch (_: IllegalArgumentException) {
                emptyList()
            }
        }
        ?: emptyList()

    private companion object {
        const val KEY_QUERIES = "search_history_queries"

        /** How many phrases are kept. Older ones fall off the end. */
        const val MAX_QUERIES = 20
    }
}
