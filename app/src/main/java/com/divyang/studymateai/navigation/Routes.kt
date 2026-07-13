package com.divyang.studymateai.navigation

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object SignUp : Routes("signup")


    // Main Tabs (Bottom Navigation)
    object Home : Routes("home")
    object Library : Routes("library")
    object History : Routes("history")
    object Profile : Routes("profile")
    object ForgetPassword : Routes("forgot_password")

    object EditProfile : Routes("editProfile")
    object ChangeEmail : Routes("changeEmail")
    object ChangePassword : Routes("changePassword")
    object AppSettings : Routes("appSettings")
    object GeminiKeySettings : Routes("geminiKeySettings")
    object PrivacyPolicy : Routes("privacyPolicy")

    // Chapter Flow
    object Scan {
        const val route = "scan"
        const val ARG_FROM_CAMERA = "fromCamera"

        // When true, the scan flow hands the extracted text back to the
        // previous back-stack entry (KEY_SCANNED_TEXT) instead of opening its
        // own editor — used by the text editor's "Scan More".
        const val ARG_RETURN_RESULT = "returnResult"

        // Pattern registered in the NavHost; also what popUpTo must reference.
        const val fullRoute =
            "$route?$ARG_FROM_CAMERA={$ARG_FROM_CAMERA}&$ARG_RETURN_RESULT={$ARG_RETURN_RESULT}"

        fun createRoute(fromCamera: Boolean, returnResult: Boolean = false): String =
            "$route?$ARG_FROM_CAMERA=$fromCamera&$ARG_RETURN_RESULT=$returnResult"
    }

    // Only the chapter id travels through the route; the editor loads the
    // chapter itself. Chapter text in nav args crashed route matching
    // (newlines break the arg pattern) and risks TransactionTooLargeException.
    object TextEdit : Routes("textEditor?chapterId={chapterId}") {
        const val ARG_CHAPTER_ID = "chapterId"
        fun createRoute(chapterId: String? = null): String =
            if (chapterId != null) "textEditor?$ARG_CHAPTER_ID=$chapterId" else "textEditor"
    }

    object ChapterView : Routes("chapter/{id}") {
        const val ARG_CHAPTER_ID = "id"
        fun createRoute(id: String) = "chapter/$id"
    }

    object ChapterDetail : Routes("chapterDetail/{chapterId}") {
        const val CHAPTER_ID = "chapterId"
        fun createRoute(chapterId: String) = "chapterDetail/$chapterId"
    }

    // Generation Screens
    object QuizGen : Routes("quiz/{chapterId}") {
        const val CHAPTER_ID = "chapterId"
        fun createRoute(chapterId: String) = "quiz/$chapterId"
    }

    object Summary : Routes("summary/{chapterId}") {
        const val CHAPTER_ID = "chapterId"
        fun createRoute(chapterId: String) = "summary/$chapterId"
    }

    object Flashcards : Routes("flashcards/{chapterId}") {
        const val ARG_CHAPTER_ID = "chapterId"
        fun createRoute(chapterId: String) = "flashcards/$chapterId"
    }

    object QuizHistoryDetail : Routes("quizHistory/{quizHistoryId}") {
        const val QUIZ_HISTORY_ID = "quizHistoryId"
        fun createRoute(quizHistoryId: String) = "quizHistory/$quizHistoryId"
    }

    companion object {
        // SavedStateHandle keys used to signal earlier back-stack entries to
        // refresh, instead of recreating their ViewModels via re-navigation.
        const val KEY_CHAPTERS_CHANGED = "chapters_changed"
        const val KEY_CHAPTER_CHANGED = "chapter_changed"

        // Scan-flow result: extracted text handed back to the text editor.
        const val KEY_SCANNED_TEXT = "scanned_text"

        fun getStartDestination(isLoggedIn: Boolean): String {
            return if (isLoggedIn) Home.route else Login.route
        }
    }
}