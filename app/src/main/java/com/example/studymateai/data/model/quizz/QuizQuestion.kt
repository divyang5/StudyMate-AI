package com.example.studymateai.data.model.quizz

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String
)