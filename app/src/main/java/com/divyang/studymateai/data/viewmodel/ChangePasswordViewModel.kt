package com.divyang.studymateai.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    fun updateCurrentPassword(value: String) = _uiState.update { it.copy(currentPassword = value) }
    fun updateNewPassword(value: String) = _uiState.update { it.copy(newPassword = value) }
    fun updateConfirmPassword(value: String) = _uiState.update { it.copy(confirmPassword = value) }

    fun changePassword() {
        viewModelScope.launch {
            val s = _uiState.value
            try {
                _uiState.update { it.copy(isLoading = true) }

                if (s.newPassword != s.confirmPassword) throw Exception("Passwords don't match")
                if (s.newPassword.length < 6) throw Exception("Password should be at least 6 characters")

                val email = authRepository.currentEmail ?: throw Exception("User not logged in")
                authRepository.reauthenticate(email, s.currentPassword)
                authRepository.updatePassword(s.newPassword)

                _toastMessage.emit("Password changed successfully!")
                _uiState.value = ChangePasswordUiState()   // clear form + loading
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _toastMessage.emit("Current password is incorrect")
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Failed to change password")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
