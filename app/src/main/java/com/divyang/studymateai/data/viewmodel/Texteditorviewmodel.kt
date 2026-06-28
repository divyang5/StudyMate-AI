package com.divyang.studymateai.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TextEditorUiState(
    val title: String = "",
    val description: String = "",
    val content: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val showValidationErrors: Boolean = false
)

class TextEditorViewModel : ViewModel() {

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
                val userId = Firebase.auth.currentUser?.uid
                    ?: throw Exception("User not logged in")
                val db = Firebase.firestore

                val chapterId = existingChapterId
                if (chapterId != null) {
                    // ── EDIT MODE: update existing document ──
                    db.collection("chapters")
                        .document(chapterId)
                        .update(
                            mapOf(
                                "title" to state.title,
                                "description" to state.description,
                                "content" to state.content,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        .await()
                } else {
                    // ── CREATE MODE: add new document ──
                    db.collection("chapters")
                        .add(
                            hashMapOf(
                                "title" to state.title,
                                "description" to state.description,
                                "content" to state.content,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "userId" to userId
                            )
                        )
                        .await()
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