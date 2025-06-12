package com.example.studymateai.navigation

import java.net.URLEncoder

sealed class Routes(val route: String) {
    object Login : Routes("login")
    object SignUp : Routes("signup")


    // Main Tabs (Bottom Navigation)
    object Home : Routes("home")
    object Library : Routes("library")
    object History : Routes("history")
    object Profile : Routes("profile")


    // Chapter Flow
    object Scan : Routes("scan") {
        const val ARG_FROM_CAMERA = "fromCamera"
        fun createRoute(fromCamera: Boolean) = "scan?$ARG_FROM_CAMERA=$fromCamera"
    }

    object TextEdit : Routes("textedit/{extractedText}") {
        const val EXTRACTED_TEXT = "extractedText"
        fun createRoute(extractedText: String) = "textedit/${URLEncoder.encode(extractedText, "UTF-8")}"
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
        const val ARG_CHAPTER_ID = "chapterId"
        fun createRoute(chapterId: String) = "summary/$chapterId"
    }

    object Flashcards : Routes("flashcards/{chapterId}") {
        const val ARG_CHAPTER_ID = "chapterId"
        fun createRoute(chapterId: String) = "flashcards/$chapterId"
    }

    companion object {
        fun getStartDestination(isLoggedIn: Boolean): String {
            return if (isLoggedIn) Home.route else Login.route
        }
    }
}