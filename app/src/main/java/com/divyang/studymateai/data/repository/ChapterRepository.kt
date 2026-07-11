package com.divyang.studymateai.data.repository

import com.divyang.studymateai.data.model.chapters.Chapter

/** Reads/writes the `chapters` collection. */
interface ChapterRepository {
    suspend fun getRecentChapters(userId: String, limit: Long = 5): List<Chapter>
    suspend fun getChapters(userId: String): List<Chapter>
    suspend fun getChapter(chapterId: String): Chapter
    suspend fun createChapter(userId: String, title: String, description: String, content: String): String
    suspend fun updateChapter(chapterId: String, title: String, description: String, content: String)
    suspend fun deleteChapter(chapterId: String)
    suspend fun getChapterTitle(chapterId: String): String
}
