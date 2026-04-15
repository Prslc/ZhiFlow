package com.prslc.zhiflow.ui.page.content

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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

    val tempSelectedIds = mutableStateListOf<Long>()

    var isLoading by mutableStateOf(false)

    fun loadCollections(contentId: String, contentType: ContentType) {
        viewModelScope.launch {
            isLoading = true
            repository.getCollections(contentId, contentType)
                .onSuccess { response ->
                    collectionList = response.data
                    tempSelectedIds.clear()
                    tempSelectedIds.addAll(collectionList.filter { it.isFavorited }.map { it.id })
                }
                .onFailure {
                    // TODO
                }
            isLoading = false
        }
    }

    // submit
    fun updateCollectionStatus(
        contentId: String,
        contentType: ContentType,
        onComplete: (Boolean) -> Unit
    ) {
        val initialIds = collectionList.filter { it.isFavorited }.map { it.id }.toSet()
        val currentIds = tempSelectedIds.toSet()

        val addIds = (currentIds - initialIds).toList()
        val removeIds = (initialIds - currentIds).toList()

        if (addIds.isEmpty() && removeIds.isEmpty()) {
            onComplete(currentIds.isNotEmpty())
            return
        }

        viewModelScope.launch {
            val success = repository.updateCollections(contentId, contentType, addIds, removeIds)
            if (success) {
                onComplete(currentIds.isNotEmpty())
                loadCollections(contentId, contentType)
            }
        }
    }
}