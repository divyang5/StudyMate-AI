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
    val isUpdating: Boolean = false
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

    /** Sequential rewrite of the former nested addOnCompleteListener pyramid. */
    fun updateEmail(newEmail: String, password: String) {
        if (newEmail.isEmpty() || password.isEmpty()) {
            viewModelScope.launch { _messageFlow.send("Please fill all fields") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            try {
                // 1. Refresh to get the latest verification status.
                authRepository.reloadUser()
                if (!authRepository.isEmailVerified()) {
                    _uiState.update { it.copy(isUpdating = false, isEmailVerified = false) }
                    _messageFlow.send("Please verify your current email first")
                    return@launch
                }

                val currentEmail = authRepository.currentEmail ?: ""
                // 2. Re-authenticate, 3. queue the verified email change, 4. mirror in Firestore.
                authRepository.reauthenticate(currentEmail, password)
                authRepository.verifyBeforeUpdateEmail(newEmail)

                val uid = authRepository.currentUserId
                    ?: throw IllegalStateException("No authenticated user")
                userRepository.updateEmail(uid, newEmail)

                // 5. Send verification to the new address.
                authRepository.sendEmailVerification()

                _uiState.update { it.copy(isUpdating = false, email = newEmail) }
                _messageFlow.send("Email updated! Please verify your new email")
            } catch (e: Exception) {
                Log.e("CHANGEEMAIL", "Update failed", e)
                _uiState.update { it.copy(isUpdating = false) }
                _messageFlow.send("Update failed: ${e.message}")
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
