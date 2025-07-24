package com.example.studymateai.data.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studymateai.data.model.profile.UserProfile
import com.google.firebase.auth.EmailAuthProvider
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
class ChangeEmailViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    private val _userState = MutableStateFlow(UserProfile())
    val userState = _userState.asStateFlow()

    private val _updateState = MutableStateFlow(false)
    val updateState = _updateState.asStateFlow()

    private val _messageFlow = Channel<String>()
    val messageFlow = _messageFlow.receiveAsFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                _userState.value = UserProfile(
                    uid = document.id,
                    firstName = document.getString("firstName") ?: "",
                    lastName = document.getString("lastName") ?: "",
                    email = document.getString("email") ?: auth.currentUser?.email ?: "",
                    createdAt = document.getTimestamp("createdAt")
                )
            }
    }

    fun updateEmail(newEmail: String, password: String) {
        if (newEmail.isEmpty() || password.isEmpty()) {
            viewModelScope.launch {
                _messageFlow.send("Please fill all fields")
            }
            return
        }

        _updateState.value = true
        val user = auth.currentUser
        val currentEmail = auth.currentUser?.email ?: ""

        if (user == null) {
            viewModelScope.launch {
                _updateState.value = false
                _messageFlow.send("User not authenticated")
            }
            return
        }

        // Reload user to get the latest verification status
        user.reload().addOnCompleteListener { reloadTask ->
            if (reloadTask.isSuccessful) {
                val reloadedUser = auth.currentUser
                if (reloadedUser == null || !reloadedUser.isEmailVerified) {
                    viewModelScope.launch {
                        _updateState.value = false
                        _messageFlow.send("Please verify your current email first")
                    }
                    return@addOnCompleteListener
                }

                // Re-authenticate user
                val credential = EmailAuthProvider.getCredential(currentEmail, password)
                reloadedUser.reauthenticate(credential)
                    .addOnCompleteListener { reAuthTask ->
                        if (reAuthTask.isSuccessful) {
                            // Update email in Firebase Auth
                            reloadedUser.verifyBeforeUpdateEmail(newEmail)
                                .addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        // Update email in Firestore
                                        firestore.collection("users").document(reloadedUser.uid)
                                            .update("email", newEmail)
                                            .addOnSuccessListener {
                                                // Send verification to new email
                                                reloadedUser.sendEmailVerification()
                                                    .addOnCompleteListener { verificationTask ->
                                                        viewModelScope.launch {
                                                            _updateState.value = false
                                                            if (verificationTask.isSuccessful) {
                                                                _messageFlow.send("Email updated! Please verify your new email")
                                                                _userState.value = _userState.value.copy(email = newEmail)
                                                            } else {
                                                                _messageFlow.send("Email updated but verification failed: ${verificationTask.exception?.message}")
                                                            }
                                                        }
                                                    }
                                            }
                                            .addOnFailureListener { e ->
                                                viewModelScope.launch {
                                                    _updateState.value = false
                                                    _messageFlow.send("Failed to update database: ${e.message}")

                                                    Log.e("CHANGEEMAIL", "Failed to update database", e)
                                                }
                                            }
                                    } else {
                                        viewModelScope.launch {
                                            _updateState.value = false
                                            _messageFlow.send("Update failed: ${updateTask.exception?.message}")
                                            Log.e("CHANGEEMAIL", "Update failed", updateTask.exception)
                                        }
                                    }
                                }
                        } else {
                            viewModelScope.launch {
                                _updateState.value = false
                                _messageFlow.send("Authentication failed: ${reAuthTask.exception?.message}")
                            }
                        }
                    }
            } else {
                viewModelScope.launch {
                    _updateState.value = false
                    _messageFlow.send("Failed to refresh user: ${reloadTask.exception?.message}")
                }
            }
        }
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    fun sendVerificationEmail() {
        auth.currentUser?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                viewModelScope.launch {
                    if (task.isSuccessful) {
                        _messageFlow.send("Verification email sent")
                    } else {
                        _messageFlow.send("Failed to send verification: ${task.exception?.message}")
                    }
                }
            }
    }
}


