package com.prslc.zhiflow.data.model.user

import androidx.compose.runtime.Immutable

/**
 * Which of an account's two lists of people a page is reading.
 *
 * [value] is what a navigation route carries; [path] is the segment the API names that list by.
 * They are not the same word — the API calls the people an account follows its `followees`, and
 * answers the ones following it at the same path as the follow action, under `followers`.
 */
@Immutable
enum class PeopleListKind(val value: String, val path: String) {
    FOLLOWING("following", "followees"),
    FOLLOWERS("followers", "followers"),
    ;

    companion object {
        /**
         * Names the list an identifier stands for.
         *
         * @param value The identifier, as it comes back out of a navigation route.
         * @return The list it names, or [FOLLOWING] for anything this app does not know.
         */
        fun from(value: String): PeopleListKind = entries.find { it.value == value } ?: FOLLOWING
    }
}
