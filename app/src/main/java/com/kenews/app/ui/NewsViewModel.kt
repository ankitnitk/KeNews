package com.kenews.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kenews.app.data.api.Article
import com.kenews.app.data.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    data class Success(val articles: List<Article>) : UiState()
    data class Error(val message: String) : UiState()
}

class NewsViewModel : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state

    private val _categories = MutableStateFlow<List<String>>(listOf("All"))
    val categories: StateFlow<List<String>> = _categories

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private var currentOffset = 0
    private var isLoadingMore = false

    init {
        loadCategories()
        loadArticles(reset = true)
    }

    fun selectCategory(category: String) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        loadArticles(reset = true)
    }

    fun loadMore() {
        if (isLoadingMore) return
        loadArticles(reset = false)
    }

    fun refresh() = loadArticles(reset = true)

    private fun loadCategories() {
        viewModelScope.launch {
            runCatching { RetrofitClient.api.getCategories() }
                .onSuccess { _categories.value = it.categories }
        }
    }

    private fun loadArticles(reset: Boolean) {
        viewModelScope.launch {
            if (reset) {
                currentOffset = 0
                _state.value = UiState.Loading
            }
            isLoadingMore = true
            val category = _selectedCategory.value.takeIf { it != "All" }
            runCatching { RetrofitClient.api.getArticles(category = category, offset = currentOffset) }
                .onSuccess { response ->
                    val existing = if (reset) emptyList()
                    else (_state.value as? UiState.Success)?.articles ?: emptyList()
                    currentOffset += response.articles.size
                    _state.value = UiState.Success(existing + response.articles)
                }
                .onFailure { e ->
                    if (reset) _state.value = UiState.Error(e.message ?: "Unknown error")
                }
            isLoadingMore = false
        }
    }
}
