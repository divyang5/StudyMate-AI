package com.divyang.studymateai.legal

/**
 * The current Terms & Conditions contract. Bump [VERSION] when the terms
 * change materially — every account whose Firestore `termsAcceptedVersion`
 * is older is re-gated at its next login / app start.
 */
object TermsPolicy {
    const val VERSION = 1
    const val URL = "https://studymateai-terms.netlify.app/"
}
