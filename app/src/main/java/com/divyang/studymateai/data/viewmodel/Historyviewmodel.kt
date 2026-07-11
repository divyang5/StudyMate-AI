package com.divyang.studymateai.data.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.model.quizz.QuizHistory
import com.divyang.studymateai.data.repository.AuthRepository
import com.divyang.studymateai.data.repository.QuizHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = true,
    val histories: List<QuizHistory> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val pendingDelete: QuizHistory? = null,   // non-null = show confirm dialog
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val quizHistoryRepository: QuizHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadHistorySuspend()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun loadHistory() {
        viewModelScope.launch { loadHistorySuspend() }
    }

    private suspend fun loadHistorySuspend() {
        val userId = authRepository.currentUserId ?: run {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        runCatching {
            quizHistoryRepository.getHistory(userId)
        }.onSuccess { list ->
            _uiState.update { it.copy(isLoading = false, histories = list) }
        }.onFailure { e ->
            Log.e("HistoryViewModel", "Error loading quiz history", e)
            _uiState.update { it.copy(isLoading = false, error = "Failed to load history.") }
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
                quizHistoryRepository.deleteHistory(target.id)
            }.onSuccess {
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
