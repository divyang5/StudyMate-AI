package com.divyang.studymateai.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val loginSuccess: Boolean = false,
    val infoMessage: String? = null,
    val isEmailUnverified: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null, isEmailUnverified = false) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }
    }

    fun clearGeneralError() {
        _uiState.update { it.copy(generalError = null) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun prefillFromPreviousSession(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                infoMessage = "Your session expired. Please sign in again."
            )
        }
    }

    fun login() {
        val state = _uiState.value

        val emailError = when {
            state.email.isBlank() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches() -> "Enter a valid email"
            else -> null
        }
        val passwordError = when {
            state.password.isBlank() -> "Password is required"
            state.password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }

        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null, isEmailUnverified = false) }
            try {
                val user = authRepository.signIn(state.email.trim(), state.password)
                if (user.isEmailVerified) {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                } else {
                    // Block login but leave them authenticated so we can resend verification.
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = "Please verify your email address to continue.",
                            isEmailUnverified = true
                        )
                    }
                }
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("no user record", ignoreCase = true) == true -> "No account found with this email"
                    e.message?.contains("invalid", ignoreCase = true) == true -> "Incorrect password"
                    e.message?.contains("too many requests", ignoreCase = true) == true -> "Too many attempts. Try again later"
                    e.message?.contains("network", ignoreCase = true) == true -> "Network error. Check your connection"
                    else -> "Login failed. Please try again"
                }
                _uiState.update { it.copy(isLoading = false, generalError = message) }
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                authRepository.sendEmailVerification()
                _uiState.update {
                    it.copy(
                        infoMessage = "Verification email sent! Please check your inbox.",
                        isEmailUnverified = false
                    )
                }
                // Sign out so they must log in fresh once verified.
                authRepository.signOut()
            } catch (e: Exception) {
                _uiState.update { it.copy(generalError = "Failed to send email. Try again later.") }
            }
        }
    }
}
