package com.divyang.studymateai.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val loginSuccess: Boolean = false,
    val infoMessage: String? = null,
    val isEmailUnverified: Boolean = false // <-- NEW: Tracks if we should show the "Resend" button
)

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
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
                // 1. Authenticate with Firebase
                val result = auth.signInWithEmailAndPassword(state.email.trim(), state.password).await()
                val user = result.user

                // 2. Check Verification Status
                if (user?.isEmailVerified == true) {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                } else {
                    // Block login. We leave them "authenticated" in Firebase temporarily
                    // so we can call sendEmailVerification(), but we DON'T trigger loginSuccess
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = "Please verify your email address to continue.",
                            isEmailUnverified = true // Triggers the resend button in UI
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

    // NEW: Function to resend the verification email
    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                auth.currentUser?.sendEmailVerification()?.await()
                _uiState.update {
                    it.copy(
                        infoMessage = "Verification email sent! Please check your inbox.",
                        isEmailUnverified = false // Hide button after sending to prevent spam
                    )
                }
                // Sign out so they have to log in fresh once they verify
                auth.signOut()
            } catch (e: Exception) {
                _uiState.update { it.copy(generalError = "Failed to send email. Try again later.") }
            }
        }
    }
}