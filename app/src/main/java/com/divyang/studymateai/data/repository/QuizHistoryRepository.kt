package com.divyang.studymateai.data.repository

import com.divyang.studymateai.data.model.quizz.QuizHistory

/** Reads/writes the `quizHistory` collection (joins chapter titles). */
interface QuizHistoryRepository {
    suspend fun getHistory(userId: String): List<QuizHistory>
    suspend fun deleteHistory(historyId: String)
}
