package com.divyang.studymateai.data.viewmodel


import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.model.chapters.Chapter
import com.divyang.studymateai.data.repository.ChapterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ChapterDetailUiState {
    object Loading : ChapterDetailUiState()
    data class Success(val chapter: Chapter) : ChapterDetailUiState()
    object Error : ChapterDetailUiState()
}

@HiltViewModel
class ChapterDetailViewModel @Inject constructor(
    private val chapterRepository: ChapterRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chapterId: String = checkNotNull(savedStateHandle["chapterId"])

    private val _uiState = MutableStateFlow<ChapterDetailUiState>(ChapterDetailUiState.Loading)
    val uiState: StateFlow<ChapterDetailUiState> = _uiState.asStateFlow()

    init {
        loadChapter()
    }

    fun loadChapter() {
        viewModelScope.launch {
            _uiState.value = ChapterDetailUiState.Loading
            try {
                val chapter = chapterRepository.getChapter(chapterId)
                _uiState.value = ChapterDetailUiState.Success(chapter)
            } catch (e: Exception) {
                Log.e("ChapterDetailVM", "Error loading chapter", e)
                _uiState.value = ChapterDetailUiState.Error
            }
        }
    }
}
