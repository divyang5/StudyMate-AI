package com.divyang.studymateai.data.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.model.chapters.Chapter
import com.divyang.studymateai.data.repository.AuthRepository
import com.divyang.studymateai.data.repository.ChapterRepository
import com.divyang.studymateai.data.repository.UserRepository
import com.divyang.studymateai.utils.AuthEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── UI State ───────────────────────────────────────────────────────────────

data class HomeUiState(
    val firstName: String = "",
    val lastName: String = "",
    val chapters: List<Chapter> = emptyList(),
    val isUserLoading: Boolean = true,
    val isChaptersLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    val fullName: String get() = "$firstName $lastName".trim()
    val displayName: String get() = firstName.ifBlank { "there" }
}

// ─── ViewModel ──────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val chapterRepository: ChapterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadAll(isRefresh = true)
    }

    /** Reloads only the chapter list, e.g. after an import/edit elsewhere. */
    fun refreshChapters() {
        viewModelScope.launch { loadChapters() }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun loadAll(isRefresh: Boolean = false) {
        viewModelScope.launch {
            val userJob = launch { loadUser() }
            val chaptersJob = launch { loadChapters() }
            userJob.join()
            chaptersJob.join()
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun loadUser() {
        val userId = authRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(isUserLoading = false) }
            AuthEventBus.notifySessionExpired()
            return
        }
        try {
            val profile = userRepository.getUserProfile(userId)
            _uiState.update {
                it.copy(firstName = profile.firstName, lastName = profile.lastName, isUserLoading = false)
            }
            authRepository.updateDisplayName("${profile.firstName} ${profile.lastName}".trim())
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user", e)
            _uiState.update { it.copy(isUserLoading = false, error = "Failed to load user data") }
        }
    }

    private suspend fun loadChapters() {
        val userId = authRepository.currentUserId
        if (userId == null) {
            _uiState.update { it.copy(isChaptersLoading = false) }
            AuthEventBus.notifySessionExpired()
            return
        }
        _uiState.update { it.copy(isChaptersLoading = true) }
        try {
            val list = chapterRepository.getRecentChapters(userId)
            _uiState.update { it.copy(chapters = list, isChaptersLoading = false) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chapters", e)
            _uiState.update { it.copy(isChaptersLoading = false, error = "Failed to load chapters") }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
