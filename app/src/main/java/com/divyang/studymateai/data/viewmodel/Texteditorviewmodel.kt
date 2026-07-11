package com.divyang.studymateai.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.repository.AuthRepository
import com.divyang.studymateai.data.repository.ChapterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TextEditorUiState(
    val title: String = "",
    val description: String = "",
    val content: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val showValidationErrors: Boolean = false
)

@HiltViewModel
class TextEditorViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chapterRepository: ChapterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextEditorUiState())
    val uiState: StateFlow<TextEditorUiState> = _uiState.asStateFlow()

    // null = create mode, non-null = edit mode
    private var existingChapterId: String? = null

    fun init(title: String, description: String, content: String, chapterId: String? = null) {
        existingChapterId = chapterId
        _uiState.update {
            it.copy(title = title, description = description, content = content)
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onContentChange(value: String) = _uiState.update { it.copy(content = value) }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearSuccess() = _uiState.update { it.copy(saveSuccess = false) }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank() || state.description.isBlank() || state.content.isBlank()) {
            _uiState.update { it.copy(showValidationErrors = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val userId = authRepository.currentUserId
                    ?: throw Exception("User not logged in")

                val chapterId = existingChapterId
                if (chapterId != null) {
                    chapterRepository.updateChapter(chapterId, state.title, state.description, state.content)
                } else {
                    chapterRepository.createChapter(userId, state.title, state.description, state.content)
                }

                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to save: ${e.message}"
                    )
                }
            }
        }
    }
}
