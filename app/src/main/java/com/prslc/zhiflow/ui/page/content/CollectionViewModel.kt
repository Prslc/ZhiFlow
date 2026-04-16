package com.prslc.zhiflow.ui.page.content

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ZhihuCollection
import com.prslc.zhiflow.data.repository.CollectionRepository
import kotlinx.coroutines.launch

class CollectionViewModel(private val repository: CollectionRepository) : ViewModel() {
    var collectionList by mutableStateOf<List<ZhihuCollection>>(emptyList())
        private set

    private val _selectedIds = mutableStateMapOf<Long, Boolean>()
    val selectedIds: Set<Long> get() = _selectedIds.filterValues { it }.keys

    var isLoading by mutableStateOf(false)
        private set

    fun hasChanges(): Boolean {
        val initialIds = collectionList.filter { it.isFavorited }.map { it.id }.toSet()
        return initialIds != selectedIds
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds[id] ?: false
        _selectedIds[id] = !current
    }

    fun loadCollections(contentId: String, contentType: ContentType) {
        viewModelScope.launch {
            try {
                isLoading = true
                repository.getCollections(contentId, contentType)
                    .onSuccess { response ->
                        collectionList = response.data
                        _selectedIds.clear()
                        response.data.forEach {
                            _selectedIds[it.id] = it.isFavorited
                        }
                    }
            } finally {
                isLoading = false
            }
        }
    }

    fun updateCollectionStatus(
        contentId: String,
        contentType: ContentType,
        onComplete: (isFaved: Boolean) -> Unit
    ) {
        val initialIds = collectionList.filter { it.isFavorited }.map { it.id }.toSet()
        val currentIds = selectedIds

        val addIds = (currentIds - initialIds).toList()
        val removeIds = (initialIds - currentIds).toList()

        if (addIds.isEmpty() && removeIds.isEmpty()) {
            onComplete(currentIds.isNotEmpty())
            return
        }

        viewModelScope.launch {
            isLoading = true
            try {
                val success = repository.updateCollections(contentId, contentType, addIds, removeIds)
                if (success) {
                    onComplete(currentIds.isNotEmpty())
                    loadCollections(contentId, contentType)
                }
            } finally {
                isLoading = false
            }
        }
    }
}