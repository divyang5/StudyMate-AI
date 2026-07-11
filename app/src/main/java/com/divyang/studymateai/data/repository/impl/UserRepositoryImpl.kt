package com.divyang.studymateai.data.repository.impl

import com.divyang.studymateai.data.model.profile.UserProfile
import com.divyang.studymateai.data.repository.UserRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val users get() = firestore.collection("users")

    override suspend fun getUserProfile(uid: String): UserProfile = runFirestore {
        val doc = users.document(uid).get().await()
        (doc.toObject(UserProfile::class.java) ?: UserProfile())
            .copy(uid = doc.id, isUserLoading = false)
    }

    override suspend fun createUserProfile(
        uid: String,
        firstName: String,
        lastName: String,
        email: String
    ) = runFirestore {
        users.document(uid).set(
            hashMapOf(
                "uid" to uid,
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }

    override suspend fun updateName(uid: String, firstName: String, lastName: String) = runFirestore {
        users.document(uid).update(
            mapOf("firstName" to firstName, "lastName" to lastName)
        ).await()
        Unit
    }

    override suspend fun updateEmail(uid: String, newEmail: String) = runFirestore {
        users.document(uid).update("email", newEmail).await()
        Unit
    }
}
