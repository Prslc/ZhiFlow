package com.prslc.zhiflow.ui.page.search

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.ignoreOutcome
import com.prslc.zhiflow.core.exception.onApiFailure
import com.prslc.zhiflow.data.dto.HotQueryDto
import com.prslc.zhiflow.data.dto.SearchItemDto
import com.prslc.zhiflow.data.dto.SuggestedQueryDto
import com.prslc.zhiflow.data.model.search.SearchTab
import com.prslc.zhiflow.data.repository.SearchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** How long the box waits after the last keystroke before asking for suggestions. */
private val SUGGEST_DEBOUNCE_MS = 250.milliseconds

class SearchViewModel(
    private val repository: SearchRepository,
) : ViewModel() {

    @Immutable
    data class SearchUiState(
        val items: List<SearchItemDto> = emptyList(),
        val isSearching: Boolean = false,
        val isNextLoading: Boolean = false,
        val isEnd: Boolean = false,
        val globalError: ApiException? = null,
        val loadMoreError: ApiException? = null,
    )

    /** What is in the box right now, whether or not it has been searched for. */
    var query by mutableStateOf("")
        private set

    /** The phrases to offer while the reader is typing. */
    var suggestions by mutableStateOf<List<String>>(emptyList())
        private set

    /** The phrases to offer before they have typed anything. */
    var hotQueries by mutableStateOf<List<HotQueryDto>>(emptyList())
        private set

    /** The phrase the results on screen are for; blank until one has been searched for. */
    var submittedQuery by mutableStateOf("")
        private set

    /** The narrower searches offered for the phrase the results are for. */
    var refinements by mutableStateOf<List<SuggestedQueryDto>>(emptyList())
        private set

    var uiState by mutableStateOf(SearchUiState())
        private set

    val listState = LazyListState()

    private var nextPageUrl: String? = null
    private var suggestJob: Job? = null
    private var refineJob: Job? = null

    /** The request whose answer is on screen -- a search, or a page of one -- so a newer supersedes it. */
    private var requestJob: Job? = null

    /**
     * Takes what the reader has typed so far, and asks what they might mean.
     *
     * Each keystroke replaces the one before it, so the request goes out only once they pause;
     * a blank box drops the suggestions rather than asking for an empty phrase.
     *
     * @param value The box's new contents.
     */
    fun onQueryChange(value: String) {
        query = value
        suggestJob?.cancel()

        if (value.isBlank()) {
            suggestions = emptyList()
            return
        }

        suggestJob = viewModelScope.launch {
            delay(SUGGEST_DEBOUNCE_MS)
            repository.suggest(value)
                .onSuccess { suggestions = it }
                .ignoreOutcome()
        }
    }

    /**
     * Searches for a phrase, replacing whatever results are on screen.
     *
     * @param text The phrase to search for. Blank is a no-op, so an empty submit cannot wipe the
     *   results the reader is looking at.
     * @param tab The tab to search in. It rides on the first page only; the pages after it carry
     *   their own.
     */
    fun submit(text: String, tab: SearchTab) {
        val phrase = text.trim()
        if (phrase.isEmpty()) return

        query = phrase
        submittedQuery = phrase
        suggestions = emptyList()
        nextPageUrl = null
        loadRefinements(phrase)

        // A search the reader has moved on from must not land on the one they are waiting for, and
        // neither must a page of the one before it. Both write the whole state, so whichever
        // finished last would be what they read, and the box would be naming a phrase the results
        // are not for.
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            uiState = SearchUiState(isSearching = true)

            repository.search(phrase, tab.value)
                .onSuccess { result ->
                    uiState = SearchUiState(items = result.items, isEnd = result.isEnd)
                    nextPageUrl = result.nextPageUrl
                }
                .onApiFailure { error ->
                    uiState = SearchUiState(globalError = error)
                }
        }
    }

    /** Loads the next page of the search on screen. No-op at the end, mid-load, or with none. */
    fun loadMore() {
        val url = nextPageUrl ?: return
        if (uiState.isNextLoading || uiState.isSearching || uiState.isEnd) return

        requestJob = viewModelScope.launch {
            uiState = uiState.copy(isNextLoading = true, loadMoreError = null)

            repository.search(submittedQuery, nextUrl = url)
                .onSuccess { result ->
                    uiState = uiState.copy(
                        items = uiState.items + result.items,
                        isNextLoading = false,
                        isEnd = result.isEnd,
                    )
                    nextPageUrl = result.nextPageUrl
                }
                .onApiFailure { error ->
                    uiState = uiState.copy(loadMoreError = error, isNextLoading = false)
                }
        }
    }

    /**
     * Asks for the narrower searches that go with [phrase], replacing the ones on screen.
     *
     * It goes out beside the search rather than after it: the chips are a second thought on the
     * page, and making the results wait for them would spend the reader's time on it. A failure
     * leaves the page without them, which is a state it can be in anyway.
     *
     * @param phrase The phrase the results are for.
     */
    private fun loadRefinements(phrase: String) {
        refineJob?.cancel()
        refinements = emptyList()

        refineJob = viewModelScope.launch {
            repository.suggestedQueries(phrase)
                .onSuccess { refinements = it }
                .ignoreOutcome()
        }
    }

    /**
     * Fetches the phrases to show before anything has been typed.
     *
     * Called once when the page appears. A failure leaves the box empty of suggestions, which is
     * a state it can be in anyway. Only the whole-site tab has them: a page scoped to what the
     * reader has saved would be offering them searches for things they have not.
     *
     * @param tab The tab the page searches in.
     */
    fun loadHotQueries(tab: SearchTab) {
        if (tab != SearchTab.GENERAL) return
        if (hotQueries.isNotEmpty()) return

        viewModelScope.launch {
            repository.hotQueries()
                .onSuccess { hotQueries = it }
                .ignoreOutcome()
        }
    }

    /** Empties the result list, for the reader who cleared the box. */
    fun clearResults() {
        nextPageUrl = null
        submittedQuery = ""
        refineJob?.cancel()
        refinements = emptyList()
        uiState = SearchUiState()
    }
}
