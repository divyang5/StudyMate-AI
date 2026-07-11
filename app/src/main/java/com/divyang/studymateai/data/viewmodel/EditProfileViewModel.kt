package com.divyang.studymateai.data.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.model.profile.UserProfile
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

data class EditProfileUiState(
    val profile: UserProfile = UserProfile(),
    val isUpdating: Boolean = false
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState = _uiState.asStateFlow()

    // Toast is a one-shot event, kept as a Channel rather than folded into UiState.
    private val _toastMessage = Channel<String>(Channel.BUFFERED)
    val toastMessage = _toastMessage.receiveAsFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId ?: run {
                _toastMessage.trySend("No authenticated user found")
                return@launch
            }
            try {
                val profile = userRepository.getUserProfile(userId)
                val withEmail = profile.copy(
                    email = profile.email.ifBlank { authRepository.currentEmail.orEmpty() }
                )
                _uiState.update { it.copy(profile = withEmail) }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user", e)
                _toastMessage.trySend("Failed to load profile")
            }
        }
    }

    fun updateProfile(firstName: String, lastName: String) {
        if (firstName.isBlank() || lastName.isBlank()) {
            _toastMessage.trySend("Name fields cannot be empty")
            return
        }
        val userId = authRepository.currentUserId ?: run {
            _toastMessage.trySend("No authenticated user found")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            try {
                userRepository.updateName(userId, firstName, lastName)
                authRepository.updateDisplayName("$firstName $lastName")
                _uiState.update {
                    it.copy(profile = it.profile.copy(firstName = firstName, lastName = lastName))
                }
                _toastMessage.send("Profile updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile", e)
                _toastMessage.send("Failed to update profile: ${e.message}")
            } finally {
                _uiState.update { it.copy(isUpdating = false) }
            }
        }
    }

    companion object {
        private const val TAG = "EditProfileViewModel"
    }
}
