package com.prslc.zhiflow.ui.page.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.core.network.HttpLogEntry
import com.prslc.zhiflow.core.network.HttpLogStore
import kotlinx.coroutines.launch

class HttpLogViewModel(private val httpLogStore: HttpLogStore) : ViewModel() {

    var entries by mutableStateOf<List<HttpLogEntry>>(emptyList())
        private set

    val loggingEnabled: Boolean = httpLogStore.isEnabled

    init {
        viewModelScope.launch {
            httpLogStore.entries.collect { entries = it }
        }
    }

    fun clear() {
        httpLogStore.clear()
    }
}
