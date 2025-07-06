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
import com.example.studymateai.ui.screen.chapter.ChapterDetailScreen
import com.example.studymateai.ui.screen.chapter.ScanScreen
import com.example.studymateai.ui.screen.chapter.TextEditorScreen
import com.example.studymateai.ui.screen.main.HistoryScreen
import com.example.studymateai.ui.screen.main.HomeScreen
import com.example.studymateai.ui.screen.main.LibraryScreen
import com.example.studymateai.ui.screen.main.ProfileScreen
import com.example.studymateai.ui.screen.quizz.QuizGenerationScreen
import com.example.studymateai.ui.screen.quizz.QuizHistoryDetailScreen
import com.example.studymateai.ui.screen.summary.SummaryScreen
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
            LibraryScreen(navController)
        }

        composable(Routes.History.route) {
            HistoryScreen(navController)
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
                onPrivacyClick = {},
                navController=navController
            )
        }


        composable(
            route = Routes.Scan.route + "?${Routes.Scan.ARG_FROM_CAMERA}={${Routes.Scan.ARG_FROM_CAMERA}}&${Routes.Scan.ARG_EXISTING_TEXT}={${Routes.Scan.ARG_EXISTING_TEXT}}",
            arguments = listOf(
                navArgument(Routes.Scan.ARG_FROM_CAMERA) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(Routes.Scan.ARG_EXISTING_TEXT) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val existingTextEncoded = backStackEntry.arguments?.getString(Routes.Scan.ARG_EXISTING_TEXT)
            val existingText = existingTextEncoded?.let { URLDecoder.decode(it, "UTF-8") }

            ScanScreen(
                navController= navController,
                initialText= existingText
            )
        }

        composable(Routes.ChapterView.route) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString(Routes.ChapterView.ARG_CHAPTER_ID) ?: ""
//            ChapterViewScreen(chapterId, navController)
        }



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

        composable(
            route = Routes.ChapterDetail.route,
            arguments = listOf(
                navArgument(Routes.ChapterDetail.CHAPTER_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString(Routes.ChapterDetail.CHAPTER_ID) ?: ""
            ChapterDetailScreen(navController, chapterId)
        }



        composable(
            route = Routes.QuizGen.route,
            arguments = listOf(
                navArgument(Routes.QuizGen.CHAPTER_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString(Routes.QuizGen.CHAPTER_ID) ?: ""
            QuizGenerationScreen(
                navController = navController,
                chapterId = chapterId
            )
        }

        composable(
            route = Routes.Summary.route,
            arguments = listOf(
                navArgument(Routes.Summary.CHAPTER_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString(Routes.Summary.CHAPTER_ID) ?: ""
            SummaryScreen(
                navController = navController,
                chapterId = chapterId
            )
        }

        composable(
            route = Routes.QuizHistoryDetail.route,
            arguments = listOf(
                navArgument(Routes.QuizHistoryDetail.QUIZ_HISTORY_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString(Routes.QuizHistoryDetail.QUIZ_HISTORY_ID) ?: ""
            QuizHistoryDetailScreen(
                navController = navController,
                quizHistoryId = quizId
            )
        }

    }
}