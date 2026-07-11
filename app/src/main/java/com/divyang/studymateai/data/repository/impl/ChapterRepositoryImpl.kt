package com.divyang.studymateai.data.repository.impl

import com.divyang.studymateai.data.model.chapters.Chapter
import com.divyang.studymateai.data.repository.ChapterRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChapterRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChapterRepository {

    private val chapters get() = firestore.collection("chapters")

    // limit + client-side sort mirrors the original query and avoids needing a
    // composite (userId + createdAt) Firestore index.
    override suspend fun getRecentChapters(userId: String, limit: Long): List<Chapter> = runFirestore {
        chapters.whereEqualTo("userId", userId).limit(limit).get().await()
            .documents.mapNotNull { it.toChapterOrNull() }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun getChapters(userId: String): List<Chapter> = runFirestore {
        chapters.whereEqualTo("userId", userId).get().await()
            .documents.mapNotNull { it.toChapterOrNull() }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun getChapter(chapterId: String): Chapter = runFirestore {
        val doc = chapters.document(chapterId).get().await()
        doc.toChapterOrNull() ?: throw NoSuchElementException("Chapter not found")
    }

    override suspend fun createChapter(
        userId: String,
        title: String,
        description: String,
        content: String
    ): String = runFirestore {
        val ref = chapters.add(
            hashMapOf(
                "title" to title,
                "description" to description,
                "content" to content,
                "createdAt" to FieldValue.serverTimestamp(),
                "userId" to userId
            )
        ).await()
        ref.id
    }

    override suspend fun updateChapter(
        chapterId: String,
        title: String,
        description: String,
        content: String
    ) = runFirestore {
        chapters.document(chapterId).update(
            mapOf(
                "title" to title,
                "description" to description,
                "content" to content,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }

    override suspend fun deleteChapter(chapterId: String) = runFirestore {
        chapters.document(chapterId).delete().await()
        Unit
    }

    override suspend fun getChapterTitle(chapterId: String): String = runFirestore {
        val doc = chapters.document(chapterId).get().await()
        doc.getString("title") ?: "Unknown Chapter"
    }
}

/** Maps a Firestore doc to a Chapter, or null when the required createdAt is missing. */
private fun com.google.firebase.firestore.DocumentSnapshot.toChapterOrNull(): Chapter? {
    val date = getDate("createdAt") ?: return null
    return Chapter(
        id = id,
        title = getString("title").orEmpty(),
        description = getString("description").orEmpty(),
        content = getString("content").orEmpty(),
        createdAt = date
    )
}
