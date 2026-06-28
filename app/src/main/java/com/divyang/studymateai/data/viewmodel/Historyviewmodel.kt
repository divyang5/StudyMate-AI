package com.divyang.studymateai.data.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.model.quizz.QuizHistory
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class HistoryUiState(
    val isLoading: Boolean = true,
    val histories: List<QuizHistory> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val pendingDelete: QuizHistory? = null,   // non-null = show confirm dialog
)

class HistoryViewModel : ViewModel() {

    private val auth      = Firebase.auth
    private val firestore = Firebase.firestore

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            // re-fetch chapters from Firestore
            loadHistory()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
    // ─── Load ─────────────────────────────────────────────────────────────────

    fun loadHistory() {
        val userId = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            runCatching {
                val snapshot = firestore.collection("quizHistory")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                // Collect unique chapter IDs and batch-fetch their titles
                val chapterIds = snapshot.documents
                    .mapNotNull { it.getString("chapterId") }
                    .toSet()

                val titlesMap = mutableMapOf<String, String>()
                chapterIds.forEach { id ->
                    val doc = firestore.collection("chapters").document(id).get().await()
                    titlesMap[id] = doc.getString("title") ?: "Unknown Chapter"
                }

                snapshot.documents.mapNotNull { doc ->
                    val date = doc.getDate("date") ?: return@mapNotNull null
                    val chapterId = doc.getString("chapterId") ?: ""
                    QuizHistory(
                        id           = doc.id,
                        chapterId    = chapterId,
                        score        = doc.getLong("score")?.toInt() ?: 0,
                        date         = date,
                        chapterTitle = titlesMap[chapterId] ?: "Unknown Chapter",
                    )
                }.sortedByDescending { it.date }

            }.onSuccess { list ->
                _uiState.update { it.copy(isLoading = false, histories = list) }
            }.onFailure { e ->
                Log.e("HistoryViewModel", "Error loading quiz history", e)
                _uiState.update { it.copy(isLoading = false, error = "Failed to load history.") }
            }
        }
    }

    // ─── Delete flow ──────────────────────────────────────────────────────────

    /** Called when user swipes a row — shows the confirmation dialog. */
    fun requestDelete(history: QuizHistory) {
        _uiState.update { it.copy(pendingDelete = history) }
    }

    /** Called when user taps "Cancel" in the dialog. */
    fun cancelDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    /** Called when user confirms deletion. */
    fun confirmDelete() {
        val target = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }

        viewModelScope.launch {
            runCatching {
                firestore.collection("quizHistory").document(target.id).delete().await()
            }.onSuccess {
                Log.d("HistoryViewModel", "Deleted history ${target.id}")
                _uiState.update { state ->
                    state.copy(histories = state.histories.filter { it.id != target.id })
                }
            }.onFailure { e ->
                Log.e("HistoryViewModel", "Delete failed for ${target.id}", e)
                _uiState.update { it.copy(error = "Delete failed. Please try again.") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}