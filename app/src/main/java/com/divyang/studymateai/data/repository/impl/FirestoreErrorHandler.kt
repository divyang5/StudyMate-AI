package com.divyang.studymateai.data.repository.impl

import com.divyang.studymateai.utils.AuthEventBus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wraps a Firestore call and centralizes session-expiry handling: if Firestore
 * reports the session is no longer valid (PERMISSION_DENIED / UNAUTHENTICATED),
 * broadcast it so the NavHost can route to Login — then rethrow so the caller
 * still handles the failure. Previously only HomeViewModel did this; now every
 * repository call gets it for free.
 *
 * Runs on Dispatchers.IO so document deserialization/sorting after await()
 * never resumes on the main thread (ViewModels launch on Main by default).
 */
internal suspend fun <T> runFirestore(block: suspend () -> T): T = withContext(Dispatchers.IO) {
    try {
        block()
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            e.code == FirebaseFirestoreException.Code.UNAUTHENTICATED
        ) {
            AuthEventBus.notifySessionExpired()
        }
        throw e
    }
}
