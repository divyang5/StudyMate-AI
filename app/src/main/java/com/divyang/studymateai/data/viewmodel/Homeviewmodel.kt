package com.divyang.studymateai.data.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.model.chapters.Chapter
import com.google.firebase.Firebase
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

class HomeViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    // ── Public API ───────────────────────────────────────────────────────────

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadAll(isRefresh = true)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private fun loadAll(isRefresh: Boolean = false) {
        viewModelScope.launch {
            loadUser()
            loadChapters()
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun loadUser() {
        val userId = auth.currentUser?.uid ?: return
        try {
            val doc = firestore.collection("users").document(userId).get().await()
            val first = doc.getString("firstName") ?: ""
            val last  = doc.getString("lastName")  ?: ""

            _uiState.update {
                it.copy(firstName = first, lastName = last, isUserLoading = false)
            }

            // Keep Firebase Auth display name in sync
            val request = UserProfileChangeRequest.Builder()
                .setDisplayName("$first $last")
                .build()
            auth.currentUser?.updateProfile(request)?.await()

            Log.d(TAG, "User loaded: $first $last")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user", e)
            _uiState.update { it.copy(isUserLoading = false, error = "Failed to load user data") }
        }
    }

    private suspend fun loadChapters() {
        val userId = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isChaptersLoading = true) }
        try {
            val snapshot = firestore.collection("chapters")
                .whereEqualTo("userId", userId)
                .limit(5)
                .get()
                .await()

            val list = snapshot.documents
                .mapNotNull { doc ->
                    doc.getDate("createdAt")?.let { date ->
                        Chapter(
                            id          = doc.id,
                            title       = doc.getString("title")       ?: "",
                            description = doc.getString("description") ?: "",
                            content     = doc.getString("content")     ?: "",
                            createdAt   = date
                        )
                    }
                }
                .sortedByDescending { it.createdAt }

            _uiState.update { it.copy(chapters = list, isChaptersLoading = false) }
            Log.d(TAG, "Chapters loaded: ${list.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chapters", e)
            _uiState.update {
                it.copy(isChaptersLoading = false, error = "Failed to load chapters")
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}