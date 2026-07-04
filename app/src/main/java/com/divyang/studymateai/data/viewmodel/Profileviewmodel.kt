package com.divyang.studymateai.data.viewmodel


import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.shredPrefs.SharedPref
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── UI State ──────────────────────────────────────────────────────────────────

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val avatarInitials: String = "",
    val isLoggingOut: Boolean = false,
    val logoutSuccess: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val accountDeleted: Boolean = false,
    val errorMessage: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ProfileViewModel(
    private val sharedPref: SharedPref
) : ViewModel() {

    private val auth = Firebase.auth

    // Collections that store a "userId" field pointing back to the account.
    // Confirm "flashcards" matches your actual FlashcardViewModel's collection name.
    private val userOwnedCollections = listOf("chapters", "quizHistory", "flashcards")

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser

        // ── SharedPref null/empty debug logs ──────────────────────────────────
        val rawFirstName = sharedPref.getFirstName()
        val rawLastName  = sharedPref.getLastName()



        // ── Safe fallback chain ──────────────────────────────────────────────
        val firstName = rawFirstName?.trim().orEmpty()
        val lastName  = rawLastName?.trim().orEmpty()

        // Priority: SharedPref name → Firebase displayName → "User"
        val displayName = when {
            firstName.isNotEmpty() || lastName.isNotEmpty() ->
                "$firstName $lastName".trim()
            user?.displayName?.isNotBlank() == true ->
                user.displayName!!
            else -> "User"
        }

        val initials = buildString {
            if (firstName.isNotEmpty()) append(firstName.first().uppercaseChar())
            if (lastName.isNotEmpty())  append(lastName.first().uppercaseChar())
        }.ifEmpty {
            // fallback initials from Firebase displayName
            user?.displayName
                ?.split(" ")
                ?.filter { it.isNotEmpty() }
                ?.take(2)
                ?.joinToString("") { it.first().uppercaseChar().toString() }
                .orEmpty()
                .ifEmpty { "U" }
        }

        Log.d("PROFILEVIEWMODEL", "Final displayName = '$displayName', initials = '$initials'")

        _uiState.update { state ->
            state.copy(
                displayName    = displayName,
                email          = user?.email.orEmpty(),
                avatarInitials = initials
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, errorMessage = null) }
            try {
                auth.signOut()
                _uiState.update { it.copy(isLoggingOut = false, logoutSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoggingOut = false,
                        errorMessage = e.message ?: "Logout failed. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Deletes the account permanently. Password is required and verified
     * (via reauthenticate) BEFORE anything is deleted — so a wrong password
     * never leaves the account in a half-deleted state.
     */
    fun deleteAccount(password: String) {
        val user = auth.currentUser
        if (user == null) {
            _uiState.update { it.copy(errorMessage = "No authenticated user found") }
            return
        }
        val email = user.email
        if (email.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Could not verify account email") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true, errorMessage = null) }
            try {
                // 1. Verify password FIRST. Nothing gets deleted until this succeeds.
                //    This also refreshes the session so the Auth deletion in step 4
                //    never throws a "recent login required" error.
                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential).await()

                val uid = user.uid
                val firestore = Firebase.firestore

                // 2. Delete every doc across all user-owned collections.
                for (collectionName in userOwnedCollections) {
                    val snapshot = firestore.collection(collectionName)
                        .whereEqualTo("userId", uid)
                        .get()
                        .await()

                    if (snapshot.isEmpty) continue

                    snapshot.documents.chunked(500).forEach { chunk ->
                        val batch = firestore.batch()
                        chunk.forEach { doc -> batch.delete(doc.reference) }
                        batch.commit().await()
                    }
                    Log.d("PROFILEVIEWMODEL", "Deleted ${snapshot.size()} docs from $collectionName")
                }

                // 3. Delete the root user profile document.
                firestore.collection("users").document(uid).delete().await()

                // 4. Delete the Firebase Auth account — session is fresh from
                //    step 1, so this succeeds without a recent-login error.
                user.delete().await()

                // 5. Sign out (redundant after delete(), but explicit) and wipe
                //    all locally cached data so nothing stale remains.
                auth.signOut()
                sharedPref.clearUserSession()

                Log.d("PROFILEVIEWMODEL", "Account fully deleted: $uid")
                _uiState.update { it.copy(isDeletingAccount = false, accountDeleted = true) }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.e("PROFILEVIEWMODEL", "Incorrect password on account deletion", e)
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        errorMessage = "Incorrect password. Please try again."
                    )
                }
            } catch (e: Exception) {
                Log.e("PROFILEVIEWMODEL", "Error deleting account", e)
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        errorMessage = "Failed to delete account: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return ProfileViewModel(SharedPref(context)) as T
        }
    }
}