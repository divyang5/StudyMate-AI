package com.divyang.studymateai.data.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.repository.AuthRepository
import com.divyang.studymateai.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null,
    val showVerificationDialog: Boolean = false,
    val signUpSuccessEmail: String = ""
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    fun onFirstNameChange(v: String) = _uiState.update { it.copy(firstName = v, firstNameError = null) }
    fun onLastNameChange(v: String) = _uiState.update { it.copy(lastName = v, lastNameError = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null, generalError = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, passwordError = null) }
    fun onConfirmPasswordChange(v: String) = _uiState.update { it.copy(confirmPassword = v, confirmPasswordError = null) }

    fun dismissVerificationDialog() {
        authRepository.signOut()
        _uiState.update { it.copy(showVerificationDialog = false) }
    }

    fun clearGeneralError() = _uiState.update { it.copy(generalError = null) }

    fun signUp() {
        val s = _uiState.value

        val firstNameError = if (s.firstName.isBlank()) "First name is required" else null
        val lastNameError = if (s.lastName.isBlank()) "Last name is required" else null
        val emailError = when {
            s.email.isBlank() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(s.email).matches() -> "Enter a valid email"
            else -> null
        }
        val passwordError = when {
            s.password.length < 8 -> "At least 8 characters"
            !s.password.any { it.isDigit() } -> "Must include a number"
            !s.password.any { it.isLetter() } -> "Must include a letter"
            else -> null
        }
        val confirmPasswordError = when {
            s.confirmPassword.isBlank() -> "Please confirm your password"
            s.confirmPassword != s.password -> "Passwords don't match"
            else -> null
        }

        if (listOf(firstNameError, lastNameError, emailError, passwordError, confirmPasswordError).any { it != null }) {
            _uiState.update {
                it.copy(
                    firstNameError = firstNameError,
                    lastNameError = lastNameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            try {
                val uid = authRepository.signUp(s.email.trim(), s.password)
                userRepository.createUserProfile(
                    uid = uid,
                    firstName = s.firstName.trim(),
                    lastName = s.lastName.trim(),
                    email = s.email.trim()
                )
                authRepository.sendEmailVerification()

                _uiState.update {
                    it.copy(isLoading = false, showVerificationDialog = true, signUpSuccessEmail = s.email.trim())
                }
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("email address is already") == true -> "An account already exists with this email"
                    e.message?.contains("network") == true -> "Network error. Check your connection"
                    else -> "Sign up failed. Please try again"
                }
                _uiState.update { it.copy(isLoading = false, generalError = message) }
            }
        }
    }
}
