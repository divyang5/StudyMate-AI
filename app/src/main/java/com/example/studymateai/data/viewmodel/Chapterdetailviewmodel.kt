package com.example.studymateai.data.viewmodel


import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studymateai.data.model.chapters.Chapter
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

sealed class ChapterDetailUiState {
    object Loading : ChapterDetailUiState()
    data class Success(val chapter: Chapter) : ChapterDetailUiState()
    object Error : ChapterDetailUiState()
}

class ChapterDetailViewModel(
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
                val document = Firebase.firestore
                    .collection("chapters")
                    .document(chapterId)
                    .get()
                    .await()

                val chapter = Chapter(
                    id = document.id,
                    title = document.getString("title") ?: "",
                    description = document.getString("description") ?: "",
                    content = document.getString("content") ?: "",
                    createdAt = document.getDate("createdAt") ?: Date()
                )
                _uiState.value = ChapterDetailUiState.Success(chapter)
            } catch (e: Exception) {
                Log.e("ChapterDetailVM", "Error loading chapter", e)
                _uiState.value = ChapterDetailUiState.Error
            }
        }
    }
}