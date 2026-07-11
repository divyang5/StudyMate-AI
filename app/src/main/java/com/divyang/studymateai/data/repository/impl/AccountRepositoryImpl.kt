package com.divyang.studymateai.data.repository.impl

import com.divyang.studymateai.data.repository.AccountRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AccountRepository {

    // Collections that store a "userId" field pointing back to the account.
    private val userOwnedCollections = listOf("chapters", "quizHistory", "flashcards")

    override suspend fun deleteAccountData(userId: String) = runFirestore {
        // Delete every doc across all user-owned collections in batches of 500.
        for (collectionName in userOwnedCollections) {
            val snapshot = firestore.collection(collectionName)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            if (snapshot.isEmpty) continue

            snapshot.documents.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
        }

        // Delete the root user profile document.
        firestore.collection("users").document(userId).delete().await()
        Unit
    }
}
