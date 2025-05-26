package com.example.studymateai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.studymateai.shredPrefs.SharedPref
import com.example.studymateai.ui.screen.LoginScreen
import com.example.studymateai.ui.screen.SignUpScreen
import com.example.studymateai.ui.screen.main.HomeScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun StudyMateNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.getStartDestination(Firebase.auth.currentUser != null),
    modifier: Modifier
) {
    val auth: FirebaseAuth = Firebase.auth
    val context=LocalContext.current
    val sharedPref = remember { SharedPref(context) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { screenSlideIn() },
        exitTransition = { screenFadeOut() },
        popEnterTransition = { screenFadeIn() },
        popExitTransition = { screenSlideOut() },
    ) {
        // Auth Screens
        composable(Routes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                },
                onSignUpClick = { navController.navigate(Routes.SignUp.route) },
                onForgotPasswordClick = {}
            )
        }

        composable(Routes.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                },
                onLoginClick = { navController.popBackStack() }
            )
        }

        // Main Tabs
        composable(Routes.Home.route) {
            HomeScreen(navController)
        }

        composable(Routes.Library.route) {
//            LibraryScreen(navController)
        }

        composable(Routes.History.route) {
//            HistoryScreen(navController)
        }

        composable(Routes.Profile.route) {
//            ProfileScreen(
//                onLogout = {
//                    auth.signOut()
//                    sharedPref.clearUserSession()
//                    navController.navigate(Routes.Login.route) {
//                        popUpTo(0)
//                    }
//                }
//            )
        }

        // Chapter Flow
        composable(Routes.Scan.route) { backStackEntry ->
            val fromCamera = backStackEntry.arguments?.getBoolean(Routes.Scan.ARG_FROM_CAMERA) ?: false
//            ScanScreen(fromCamera, navController)
        }

        composable(Routes.ChapterView.route) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString(Routes.ChapterView.ARG_CHAPTER_ID) ?: ""
//            ChapterViewScreen(chapterId, navController)
        }

        // Generation Screens
        composable(Routes.QuizGen.route) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString(Routes.QuizGen.ARG_CHAPTER_ID) ?: ""
//            QuizGenerationScreen(chapterId, navController)
        }

        // Add similar composable entries for Summary and Flashcards
    }
}