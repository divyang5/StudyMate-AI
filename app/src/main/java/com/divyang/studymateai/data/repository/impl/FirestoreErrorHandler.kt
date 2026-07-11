package com.divyang.studymateai.data.repository.impl

import com.divyang.studymateai.utils.AuthEventBus
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Wraps a Firestore call and centralizes session-expiry handling: if Firestore
 * reports the session is no longer valid (PERMISSION_DENIED / UNAUTHENTICATED),
 * broadcast it so the NavHost can route to Login — then rethrow so the caller
 * still handles the failure. Previously only HomeViewModel did this; now every
 * repository call gets it for free.
 */
internal suspend fun <T> runFirestore(block: suspend () -> T): T {
    try {
        return block()
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            e.code == FirebaseFirestoreException.Code.UNAUTHENTICATED
        ) {
            AuthEventBus.notifySessionExpired()
        }
        throw e
    }
}
