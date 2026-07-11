package com.divyang.studymateai.data.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.model.chapters.Chapter
import com.divyang.studymateai.data.repository.AuthRepository
import com.divyang.studymateai.data.repository.ChapterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// In LibraryUiState
data class LibraryUiState(
    val chapters: List<Chapter> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)


// Computed from UiState — no extra state needed
val LibraryUiState.filteredChapters: List<Chapter>
    get() = if (searchQuery.isBlank()) chapters
    else chapters.filter { chapter ->
        chapter.title.contains(searchQuery, ignoreCase = true) ||
                chapter.description.contains(searchQuery, ignoreCase = true)
    }


@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chapterRepository: ChapterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadChapters()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadChaptersSuspend()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteChapter(chapterId: String) {
        viewModelScope.launch {
            try {
                chapterRepository.deleteChapter(chapterId)
                _uiState.update { state ->
                    state.copy(chapters = state.chapters.filter { it.id != chapterId })
                }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error deleting chapter", e)
            }
        }
    }

    private fun loadChapters() {
        viewModelScope.launch { loadChaptersSuspend() }
    }

    private suspend fun loadChaptersSuspend() {
        val userId = authRepository.currentUserId ?: run {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val fetched = chapterRepository.getChapters(userId)
            _uiState.update { it.copy(chapters = fetched, isLoading = false) }
        } catch (e: Exception) {
            Log.e("LibraryViewModel", "Error fetching chapters", e)
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
