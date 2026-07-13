package com.divyang.studymateai.data.model.profile

import com.google.firebase.Timestamp

data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val createdAt: Timestamp? = null,
    val termsAcceptedVersion: Int = 0,
    val termsAcceptedAt: Timestamp? = null,
    val isUserLoading: Boolean = true,
    val error: String? = null

)