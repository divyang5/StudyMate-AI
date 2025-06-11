package com.example.studymateai.data.model.chapters

import java.util.Date

data class Chapter(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val content: String = "",
    val createdAt: Date = Date()
)
