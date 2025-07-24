package com.example.studymateai.data.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChangePasswordViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    var currentPassword by mutableStateOf("")
        private set
    var newPassword by mutableStateOf("")
        private set
    var confirmPassword by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    fun updateCurrentPassword(value: String) {
        currentPassword = value
    }

    fun updateNewPassword(value: String) {
        newPassword = value
    }

    fun updateConfirmPassword(value: String) {
        confirmPassword = value
    }

    fun changePassword() {
        viewModelScope.launch {
            try {
                isLoading = true

                // Validate inputs
                if (newPassword != confirmPassword) {
                    throw Exception("Passwords don't match")
                }

                if (newPassword.length < 6) {
                    throw Exception("Password should be at least 6 characters")
                }

                // Reauthenticate user
                val user = auth.currentUser ?: throw Exception("User not logged in")
                val credential = EmailAuthProvider.getCredential(
                    user.email!!,
                    currentPassword
                )
                user.reauthenticate(credential).await()

                // Change password
                user.updatePassword(newPassword).await()

                _toastMessage.emit("Password changed successfully!")
                clearForm()
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _toastMessage.emit("Current password is incorrect")
            } catch (e: Exception) {
                _toastMessage.emit(e.message ?: "Failed to change password")
            } finally {
                isLoading = false
            }
        }
    }

    private fun clearForm() {
        currentPassword = ""
        newPassword = ""
        confirmPassword = ""
    }
}