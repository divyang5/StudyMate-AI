package com.divyang.studymateai.data.model.quizz

import com.google.firebase.firestore.Exclude
import java.util.Date

data class QuizHistory(
    val id: String = "",
    val chapterId: String = "",
    val score: Int = 0,
    val date: Date = Date(),
    // Join field populated from the chapters collection — not stored on the quizHistory doc.
    @get:Exclude val chapterTitle: String = ""
)
