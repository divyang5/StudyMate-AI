package com.divyang.studymateai.data.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.repository.AuthRepository
import com.divyang.studymateai.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangeEmailUiState(
    val uid: String = "",
    val email: String = "",
    val isEmailVerified: Boolean = false,
    val isUpdating: Boolean = false,
    // Non-null once a verification link is on its way to the new address —
    // the screen switches to the "check your inbox" state.
    val verificationSentTo: String? = null
)

@HiltViewModel
class ChangeEmailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangeEmailUiState())
    val uiState = _uiState.asStateFlow()

    private val _messageFlow = Channel<String>()
    val messageFlow = _messageFlow.receiveAsFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            try {
                val profile = userRepository.getUserProfile(userId)
                _uiState.update {
                    it.copy(
                        uid = profile.uid.ifBlank { userId },
                        email = profile.email.ifBlank { authRepository.currentEmail.orEmpty() },
                        isEmailVerified = authRepository.isEmailVerified()
                    )
                }
            } catch (e: Exception) {
                Log.e("CHANGEEMAIL", "Failed to load profile", e)
                _messageFlow.send("Failed to load profile")
            }
        }
    }

    /**
     * Starts a verified email change: re-authenticate with the password, then
     * let Firebase send a verification link to the NEW address. Nothing is
     * changed yet — the email (and our Firestore mirror, synced at next
     * login) only updates after the user clicks that link, at which point
     * Firebase revokes the session and they sign back in with the new email.
     */
    fun requestEmailChange(newEmail: String, password: String) {
        if (newEmail.isEmpty() || password.isEmpty()) {
            viewModelScope.launch { _messageFlow.send("Please fill all fields") }
            return
        }
        if (newEmail.equals(_uiState.value.email, ignoreCase = true)) {
            viewModelScope.launch { _messageFlow.send("That is already your current email") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            try {
                // Refresh to get the latest verification status.
                authRepository.reloadUser()
                if (!authRepository.isEmailVerified()) {
                    _uiState.update { it.copy(isUpdating = false, isEmailVerified = false) }
                    _messageFlow.send("Please verify your current email first")
                    return@launch
                }

                val currentEmail = authRepository.currentEmail ?: ""
                authRepository.reauthenticate(currentEmail, password)
                authRepository.verifyBeforeUpdateEmail(newEmail)

                _uiState.update {
                    it.copy(isUpdating = false, verificationSentTo = newEmail)
                }
            } catch (e: Exception) {
                Log.e("CHANGEEMAIL", "Email change request failed", e)
                val message = when {
                    e.message?.contains("credential", ignoreCase = true) == true ||
                        e.message?.contains("password", ignoreCase = true) == true ->
                        "Incorrect password"
                    e.message?.contains("already in use", ignoreCase = true) == true ->
                        "This email is already used by another account"
                    e.message?.contains("badly formatted", ignoreCase = true) == true ->
                        "Enter a valid email address"
                    e.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Check your connection"
                    else -> "Couldn't start the email change. Please try again"
                }
                _uiState.update { it.copy(isUpdating = false) }
                _messageFlow.send(message)
            }
        }
    }

    fun sendVerificationEmail() {
        viewModelScope.launch {
            try {
                authRepository.sendEmailVerification()
                _messageFlow.send("Verification email sent")
            } catch (e: Exception) {
                _messageFlow.send("Failed to send verification: ${e.message}")
            }
        }
    }
}
