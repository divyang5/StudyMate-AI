package com.example.studymateai.data.model.profile

import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val createdAt: Timestamp? = null
)