package com.divyang.studymateai.fakes

import com.divyang.studymateai.data.model.quizz.QuizHistory
import com.divyang.studymateai.data.repository.AuthRepository
import com.divyang.studymateai.data.repository.QuizHistoryRepository
import com.google.firebase.auth.FirebaseUser

/**
 * Hand-written fakes — the whole point of the repository layer: ViewModels can now
 * be unit-tested with zero Firebase.
 */
class FakeAuthRepository(
    private val userId: String? = "test-uid",
    private val email: String? = "test@example.com",
    private val verified: Boolean = true
) : AuthRepository {
    override val currentUserId: String? get() = userId
    override val currentEmail: String? get() = email
    override val currentDisplayName: String? get() = "Test User"
    override fun isEmailVerified(): Boolean = verified

    override suspend fun signIn(email: String, password: String): FirebaseUser = throw NotImplementedError()
    override suspend fun signUp(email: String, password: String): String = throw NotImplementedError()
    override suspend fun sendEmailVerification() = Unit
    override suspend fun sendPasswordReset(email: String) = Unit
    override suspend fun reloadUser() = Unit
    override suspend fun reauthenticate(email: String, password: String) = Unit
    override suspend fun verifyBeforeUpdateEmail(newEmail: String) = Unit
    override suspend fun updatePassword(newPassword: String) = Unit
    override suspend fun updateDisplayName(displayName: String) = Unit
    override suspend fun deleteCurrentUser() = Unit
    override fun signOut() = Unit
}

class FakeQuizHistoryRepository(
    initial: List<QuizHistory> = emptyList()
) : QuizHistoryRepository {
    val items = initial.toMutableList()

    override suspend fun getHistory(userId: String): List<QuizHistory> = items.toList()

    override suspend fun deleteHistory(historyId: String) {
        items.removeAll { it.id == historyId }
    }
}
