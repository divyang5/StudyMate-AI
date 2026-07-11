package com.divyang.studymateai.data.repository

import com.google.firebase.auth.FirebaseUser

/** Abstraction over FirebaseAuth so ViewModels never touch the SDK directly. */
interface AuthRepository {
    val currentUserId: String?
    val currentEmail: String?
    val currentDisplayName: String?
    fun isEmailVerified(): Boolean

    suspend fun signIn(email: String, password: String): FirebaseUser
    suspend fun signUp(email: String, password: String): String   // returns uid
    suspend fun sendEmailVerification()
    suspend fun sendPasswordReset(email: String)
    suspend fun reloadUser()
    suspend fun reauthenticate(email: String, password: String)
    suspend fun verifyBeforeUpdateEmail(newEmail: String)
    suspend fun updatePassword(newPassword: String)
    suspend fun updateDisplayName(displayName: String)
    suspend fun deleteCurrentUser()
    fun signOut()
}
