package com.example.studymateai.data.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studymateai.shredPrefs.SharedPref
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI State ──────────────────────────────────────────────────────────────────

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val avatarInitials: String = "",
    val isLoggingOut: Boolean = false,
    val logoutSuccess: Boolean = false,
    val errorMessage: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ProfileViewModel(
    private val sharedPref: SharedPref
) : ViewModel() {

    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        val firstName = sharedPref.getFirstName().orEmpty()
        val lastName  = sharedPref.getLastName().orEmpty()

        val initials = buildString {
            if (firstName.isNotEmpty()) append(firstName.first().uppercaseChar())
            if (lastName.isNotEmpty())  append(lastName.first().uppercaseChar())
        }.ifEmpty { "U" }

        _uiState.update { state ->
            state.copy(
                displayName   = user?.displayName ?: "$firstName $lastName".trim().ifEmpty { "User" },
                email         = user?.email ?: "",
                avatarInitials = initials
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, errorMessage = null) }
            try {
                auth.signOut()
                _uiState.update { it.copy(isLoggingOut = false, logoutSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoggingOut  = false,
                        errorMessage  = e.message ?: "Logout failed. Please try again."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return ProfileViewModel(SharedPref(context)) as T
        }
    }
}