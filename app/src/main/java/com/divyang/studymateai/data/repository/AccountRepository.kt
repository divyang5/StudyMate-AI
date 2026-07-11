package com.divyang.studymateai.data.repository

/** Cross-collection account operations (e.g. cascading data deletion). */
interface AccountRepository {
    /** Deletes all Firestore data owned by the user plus their users/{uid} profile doc. */
    suspend fun deleteAccountData(userId: String)
}
