package com.divyang.studymateai.data.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.model.profile.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _userState = MutableStateFlow(UserProfile())
    val userState = _userState.asStateFlow()

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating = _isUpdating.asStateFlow()

    private val _toastMessage = Channel<String>(Channel.BUFFERED)
    val toastMessage = _toastMessage.receiveAsFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: run {
                _toastMessage.trySend("No authenticated user found")
                return@launch
            }
            try {
                val doc = firestore.collection("users").document(userId).get().await()
                _userState.update {
                    it.copy(
                        uid = doc.id,
                        firstName = doc.getString("firstName") ?: "",
                        lastName = doc.getString("lastName") ?: "",
                        email = doc.getString("email") ?: auth.currentUser?.email ?: "",
                        createdAt = doc.getTimestamp("createdAt")
                    )
                }
                Log.d(TAG, "User loaded: ${_userState.value.firstName} ${_userState.value.lastName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user", e)
                _userState.update { it.copy(error = "Failed to load user data") }
                _toastMessage.trySend("Failed to load profile")
            }
        }
    }

    fun updateProfile(firstName: String, lastName: String) {
        if (firstName.isBlank() || lastName.isBlank()) {
            _toastMessage.trySend("Name fields cannot be empty")
            return
        }

        val userId = auth.currentUser?.uid ?: run {
            _toastMessage.trySend("No authenticated user found")
            return
        }

        viewModelScope.launch {
            _isUpdating.value = true
            try {
                // Update Firestore
                firestore.collection("users").document(userId)
                    .update(mapOf("firstName" to firstName, "lastName" to lastName))
                    .await()

                // Keep Firebase Auth display name in sync
                val request = UserProfileChangeRequest.Builder()
                    .setDisplayName("$firstName $lastName")
                    .build()
                auth.currentUser?.updateProfile(request)?.await()

                // Update local state
                _userState.update { it.copy(firstName = firstName, lastName = lastName) }
                _toastMessage.send("Profile updated successfully")
                Log.d(TAG, "Profile updated: $firstName $lastName")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile", e)
                _toastMessage.send("Failed to update profile: ${e.message}")
            } finally {
                _isUpdating.value = false
            }
        }
    }

    companion object {
        private const val TAG = "EditProfileViewModel"
    }
}