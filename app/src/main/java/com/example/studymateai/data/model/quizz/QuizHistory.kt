package com.example.studymateai.data.model.quizz

import java.util.Date

data class QuizHistory(
    val id: String,
    val chapterId: String,
    val score: Int,
    val date: Date,
    val chapterTitle: String
)
