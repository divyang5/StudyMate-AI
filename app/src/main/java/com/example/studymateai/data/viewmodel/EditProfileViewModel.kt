package com.example.studymateai.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studymateai.data.model.profile.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel()  {
    private val _userState = MutableStateFlow(UserProfile())
    val userState = _userState.asStateFlow()

    private val _updateState = MutableStateFlow(false)
    val updateState = _updateState.asStateFlow()

    private val _toastMessage = Channel<String>()
    val toastMessage = _toastMessage.receiveAsFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    _userState.value = UserProfile(
                        uid = document.id,
                        firstName = document.getString("firstName") ?: "",
                        lastName = document.getString("lastName") ?: "",
                        email = document.getString("email") ?: auth.currentUser?.email ?: "",
                        createdAt = document.getTimestamp("createdAt")
                    )
                } else {
                    // Create default profile if document doesn't exist
                    _userState.value = UserProfile(
                        uid = userId,
                        email = auth.currentUser?.email ?: ""
                    )
                    _toastMessage.trySend("Profile not found, creating new one")
                }
            }
            .addOnFailureListener { e ->
                _toastMessage.trySend("Error loading profile: ${e.message}")
            }
    }

    fun updateProfile(firstName: String, lastName: String) {
        if (firstName.isBlank() || lastName.isBlank()) {
            viewModelScope.launch {
                _toastMessage.send("Please fill all fields")
            }
            return
        }

        _updateState.value = true
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .update(
                mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName
                )
            )
            .addOnSuccessListener {
                viewModelScope.launch {
                    _updateState.value = false
                    _toastMessage.send("Profile updated successfully")
                    // Update local state
                    _userState.value = _userState.value.copy(
                        firstName = firstName,
                        lastName = lastName
                    )
                }
            }
            .addOnFailureListener { e ->
                viewModelScope.launch {
                    _updateState.value = false
                    _toastMessage.send("Error updating profile: ${e.message}")
                }
            }
    }
}