package com.prslc.zhiflow.data.model.search

import androidx.compose.runtime.Immutable

/**
 * The tab a search is run in, named as `/search_v3` takes it in `t`.
 *
 * The server offers a dozen others; these are the two this app has a way into, and so the only
 * ones whose results it has to draw. [GENERAL] searches the whole site, [FAVLIST] only what the
 * reader has saved.
 */
@Immutable
enum class SearchTab(val value: String) {
    GENERAL("general"),
    FAVLIST("favlist"),
    ;

    companion object {
        /**
         * Names the tab an identifier stands for.
         *
         * @param value The identifier, as it comes back out of a navigation route.
         * @return The tab it names, or [GENERAL] for anything this app does not know.
         */
        fun from(value: String): SearchTab = entries.find { it.value == value } ?: GENERAL
    }
}
