package com.divyang.studymateai.data.model.quizz

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String
)