package com.divyang.studymateai.data.repository.impl

import com.divyang.studymateai.data.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUserId: String? get() = auth.currentUser?.uid
    override val currentEmail: String? get() = auth.currentUser?.email
    override val currentDisplayName: String? get() = auth.currentUser?.displayName
    override fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified ?: false

    override suspend fun signIn(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: throw IllegalStateException("Sign-in returned no user")
    }

    override suspend fun signUp(email: String, password: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: throw IllegalStateException("Sign-up returned no user")
    }

    override suspend fun sendEmailVerification() {
        val user = auth.currentUser ?: throw IllegalStateException("No signed-in user")
        user.sendEmailVerification().await()
    }

    override suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun reloadUser() {
        auth.currentUser?.reload()?.await()
    }

    override suspend fun reauthenticate(email: String, password: String) {
        val user = auth.currentUser ?: throw IllegalStateException("No signed-in user")
        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).await()
    }

    override suspend fun verifyBeforeUpdateEmail(newEmail: String) {
        val user = auth.currentUser ?: throw IllegalStateException("No signed-in user")
        user.verifyBeforeUpdateEmail(newEmail).await()
    }

    override suspend fun updatePassword(newPassword: String) {
        val user = auth.currentUser ?: throw IllegalStateException("No signed-in user")
        user.updatePassword(newPassword).await()
    }

    override suspend fun updateDisplayName(displayName: String) {
        val user = auth.currentUser ?: return
        val request = UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
        user.updateProfile(request).await()
    }

    override suspend fun deleteCurrentUser() {
        auth.currentUser?.delete()?.await()
    }

    override fun signOut() = auth.signOut()
}
