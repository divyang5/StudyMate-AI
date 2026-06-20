package com.example.studymateai.data.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studymateai.data.model.chapters.Chapter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LibraryUiState(
    val chapters: List<Chapter> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
)

// Computed from UiState — no extra state needed
val LibraryUiState.filteredChapters: List<Chapter>
    get() = if (searchQuery.isBlank()) chapters
    else chapters.filter { chapter ->
        chapter.title.contains(searchQuery, ignoreCase = true) ||
                chapter.description.contains(searchQuery, ignoreCase = true)
    }


class LibraryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState = _uiState.asStateFlow()

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    init {
        fetchChapters()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteChapter(chapterId: String) {
        firestore.collection("chapters").document(chapterId)
            .delete()
            .addOnSuccessListener {
                _uiState.update { state ->
                    state.copy(chapters = state.chapters.filter { it.id != chapterId })
                }
            }
            .addOnFailureListener { e ->
                Log.e("LibraryViewModel", "Error deleting chapter", e)
            }
    }

    private fun fetchChapters() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("chapters")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val fetched = snapshot.documents
                    .mapNotNull { doc ->
                        val date = doc.getDate("createdAt") ?: return@mapNotNull null
                        Chapter(
                            id = doc.id,
                            title = doc.getString("title").orEmpty(),
                            description = doc.getString("description").orEmpty(),
                            content = doc.getString("content").orEmpty(),
                            createdAt = date
                        )
                    }
                    .sortedByDescending { it.createdAt }

                _uiState.update { it.copy(chapters = fetched, isLoading = false) }
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error fetching chapters", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}