package com.prslc.zhiflow.ui.page.content

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ZhihuCollection
import com.prslc.zhiflow.data.repository.CollectionRepository
import kotlinx.coroutines.launch

class CollectionViewModel(private val repository: CollectionRepository) : ViewModel() {

    @Stable
    data class CollectionUiState(
        val collections: List<ZhihuCollection> = emptyList(),
        val selectedIds: Map<Long, Boolean> = emptyMap(),
        val isLoading: Boolean = false
    )

    var uiState by mutableStateOf(CollectionUiState())
        private set

    val selectedIds: Set<Long> get() = uiState.selectedIds.filterValues { it }.keys

    fun hasChanges(): Boolean {
        val initialIds = uiState.collections.filter { it.isFavorited }.map { it.id }.toSet()
        return initialIds != selectedIds
    }

    fun toggleSelection(id: Long) {
        val current = uiState.selectedIds[id] ?: false
        uiState = uiState.copy(
            selectedIds = uiState.selectedIds + (id to !current)
        )
    }

    fun loadCollections(contentId: String, contentType: ContentType) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            repository.getCollections(contentId, contentType)
                .onSuccess { response ->
                    uiState = uiState.copy(
                        collections = response.data,
                        selectedIds = response.data.associate { it.id to it.isFavorited },
                        isLoading = false
                    )
                }
                .onFailure {
                    uiState = uiState.copy(isLoading = false)
                }
        }
    }

    fun updateCollectionStatus(
        contentId: String,
        contentType: ContentType,
        onComplete: (isFaved: Boolean) -> Unit
    ) {
        val initialIds = uiState.collections.filter { it.isFavorited }.map { it.id }.toSet()
        val currentIds = selectedIds

        val addIds = (currentIds - initialIds).toList()
        val removeIds = (initialIds - currentIds).toList()

        if (addIds.isEmpty() && removeIds.isEmpty()) {
            onComplete(currentIds.isNotEmpty())
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            try {
                val success = repository.updateCollections(contentId, contentType, addIds, removeIds)
                if (success) {
                    onComplete(currentIds.isNotEmpty())
                    loadCollections(contentId, contentType)
                }
            } finally {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }
}
