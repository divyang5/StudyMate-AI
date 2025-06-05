package com.example.studymateai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.studymateai.shredPrefs.SharedPref
import com.example.studymateai.ui.screen.LoginScreen
import com.example.studymateai.ui.screen.SignUpScreen
import com.example.studymateai.ui.screen.chapter.ScanScreen
import com.example.studymateai.ui.screen.chapter.TextEditorScreen
import com.example.studymateai.ui.screen.main.HomeScreen
import com.example.studymateai.ui.screen.main.ProfileScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.net.URLDecoder

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
            ProfileScreen(
                onLogout = {
                    auth.signOut()
                    sharedPref.clearUserSession()
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0)
                    }
                },
                onPasswordChange = {},
                onSettingsClick = {},
                onPrivacyClick = {}
            )
        }


        composable(
            route = Routes.Scan.route,
            arguments = listOf(
                navArgument(Routes.Scan.ARG_FROM_CAMERA) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val fromCamera = backStackEntry.arguments?.getBoolean(Routes.Scan.ARG_FROM_CAMERA) ?: false
            ScanScreen(fromCamera = fromCamera, navController = navController)
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
        composable(
            route = Routes.TextEdit.route,
            arguments = listOf(
                navArgument(Routes.TextEdit.EXTRACTED_TEXT) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val encodedText = backStackEntry.arguments?.getString(Routes.TextEdit.EXTRACTED_TEXT) ?: ""
            val extractedText = try {
                URLDecoder.decode(encodedText, "UTF-8")
            } catch (e: Exception) {
                ""
            }

            TextEditorScreen(
                extractedText = extractedText,
                navController = navController
            )
        }
    }
}