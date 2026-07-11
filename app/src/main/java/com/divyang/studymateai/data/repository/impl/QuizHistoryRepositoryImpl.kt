package com.divyang.studymateai.data.repository.impl

import com.divyang.studymateai.data.model.quizz.QuizHistory
import com.divyang.studymateai.data.repository.QuizHistoryRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class QuizHistoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : QuizHistoryRepository {

    override suspend fun getHistory(userId: String): List<QuizHistory> = runFirestore {
        val snapshot = firestore.collection("quizHistory")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        // Batch-fetch the chapter titles for the referenced chapterIds (join).
        val chapterIds = snapshot.documents.mapNotNull { it.getString("chapterId") }.toSet()
        val titlesMap = mutableMapOf<String, String>()
        chapterIds.forEach { id ->
            val doc = firestore.collection("chapters").document(id).get().await()
            titlesMap[id] = doc.getString("title") ?: "Unknown Chapter"
        }

        snapshot.documents.mapNotNull { doc ->
            val date = doc.getDate("date") ?: return@mapNotNull null
            val chapterId = doc.getString("chapterId") ?: ""
            QuizHistory(
                id = doc.id,
                chapterId = chapterId,
                score = doc.getLong("score")?.toInt() ?: 0,
                date = date,
                chapterTitle = titlesMap[chapterId] ?: "Unknown Chapter"
            )
        }.sortedByDescending { it.date }
    }

    override suspend fun deleteHistory(historyId: String) = runFirestore {
        firestore.collection("quizHistory").document(historyId).delete().await()
        Unit
    }
}
