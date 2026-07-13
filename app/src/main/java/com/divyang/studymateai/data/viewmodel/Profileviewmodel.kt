package com.divyang.studymateai.data.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.repository.AccountRepository
import com.divyang.studymateai.data.repository.AuthRepository
import com.divyang.studymateai.shredPrefs.SharedPref
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val avatarInitials: String = "",
    val isLoggingOut: Boolean = false,
    val logoutSuccess: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val accountDeleted: Boolean = false,
    val errorMessage: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val sharedPref: SharedPref
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val firstName = sharedPref.getFirstName()?.trim().orEmpty()
        val lastName = sharedPref.getLastName()?.trim().orEmpty()
        val fallbackName = authRepository.currentDisplayName

        // Priority: SharedPref name → Firebase displayName → "User"
        val displayName = when {
            firstName.isNotEmpty() || lastName.isNotEmpty() -> "$firstName $lastName".trim()
            !fallbackName.isNullOrBlank() -> fallbackName
            else -> "User"
        }

        val initials = buildString {
            if (firstName.isNotEmpty()) append(firstName.first().uppercaseChar())
            if (lastName.isNotEmpty()) append(lastName.first().uppercaseChar())
        }.ifEmpty {
            fallbackName
                ?.split(" ")
                ?.filter { it.isNotEmpty() }
                ?.take(2)
                ?.joinToString("") { it.first().uppercaseChar().toString() }
                .orEmpty()
                .ifEmpty { "U" }
        }

        _uiState.update { state ->
            state.copy(
                displayName = displayName,
                email = authRepository.currentEmail.orEmpty(),
                avatarInitials = initials
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, errorMessage = null) }
            try {
                authRepository.signOut()
                _uiState.update { it.copy(isLoggingOut = false, logoutSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoggingOut = false,
                        errorMessage = e.message ?: "Logout failed. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Deletes the account permanently. Password is verified (via reauthenticate)
     * BEFORE anything is deleted — so a wrong password never leaves the account
     * in a half-deleted state.
     */
    fun deleteAccount(password: String) {
        val email = authRepository.currentEmail
        if (authRepository.currentUserId == null) {
            _uiState.update { it.copy(errorMessage = "No authenticated user found") }
            return
        }
        if (email.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Could not verify account email") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true, errorMessage = null) }
            var dataDeleted = false
            try {
                // 1. Verify password FIRST (also refreshes the session for step 3).
                authRepository.reauthenticate(email, password)

                val uid = authRepository.currentUserId
                    ?: throw IllegalStateException("No authenticated user found")

                // 2. Cascade-delete all Firestore data + the user profile doc.
                accountRepository.deleteAccountData(uid)

                // Past this point the user's data is gone; if auth deletion fails
                // we're in an orphaned-auth state and need a distinct message.
                dataDeleted = true

                // 3. Delete the Firebase Auth account (session is fresh from step 1).
                authRepository.deleteCurrentUser()

                // 4. Sign out + wipe local session cache.
                authRepository.signOut()
                sharedPref.clearUserSession()

                _uiState.update { it.copy(isDeletingAccount = false, accountDeleted = true) }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.e("PROFILEVIEWMODEL", "Incorrect password on account deletion", e)
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        errorMessage = "Incorrect password. Please try again."
                    )
                }
            } catch (e: Exception) {
                Log.e("PROFILEVIEWMODEL", "Error deleting account", e)
                val message = if (dataDeleted) {
                    "Your data was removed, but the account itself could not be deleted. " +
                        "Please sign in again and retry account deletion."
                } else {
                    "Failed to delete account: ${e.message}"
                }
                _uiState.update {
                    it.copy(isDeletingAccount = false, errorMessage = message)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
