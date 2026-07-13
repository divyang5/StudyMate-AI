package com.divyang.studymateai.data.repository

import com.divyang.studymateai.data.model.profile.UserProfile

/** Reads/writes the `users` collection. */
interface UserRepository {
    suspend fun getUserProfile(uid: String): UserProfile
    suspend fun createUserProfile(uid: String, firstName: String, lastName: String, email: String)
    suspend fun updateName(uid: String, firstName: String, lastName: String)
    suspend fun updateEmail(uid: String, newEmail: String)

    /** Stamps the account with acceptance of the current terms version. */
    suspend fun recordTermsAcceptance(uid: String)
}
