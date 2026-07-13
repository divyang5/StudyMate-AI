package com.divyang.studymateai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.divyang.studymateai.shredPrefs.SharedPref
import com.divyang.studymateai.ui.screen.ForgotPasswordScreen
import com.divyang.studymateai.ui.screen.LoginScreen
import com.divyang.studymateai.ui.screen.SignUpScreen
import com.divyang.studymateai.ui.screen.chapter.ChapterDetailScreen
import com.divyang.studymateai.ui.screen.flashCard.FlashCardScreen
import com.divyang.studymateai.ui.screen.main.HistoryScreen
import com.divyang.studymateai.ui.screen.main.HomeScreen
import com.divyang.studymateai.ui.screen.main.LibraryScreen
import com.divyang.studymateai.ui.screen.main.ProfileScreen
import com.divyang.studymateai.ui.screen.profile.AppSettingsScreen
import com.divyang.studymateai.ui.screen.profile.ChangeEmailScreen
import com.divyang.studymateai.ui.screen.profile.ChangePasswordScreen
import com.divyang.studymateai.ui.screen.profile.EditProfileScreen
import com.divyang.studymateai.ui.screen.profile.PrivacyPolicyScreen
import com.divyang.studymateai.ui.screen.quizz.QuizGenerationScreen
import com.divyang.studymateai.ui.screen.quizz.QuizHistoryDetailScreen
import com.divyang.studymateai.ui.screen.scan.ScanScreen
import com.divyang.studymateai.ui.screen.scan.TextEditorScreen
import com.divyang.studymateai.ui.screen.summary.SummaryScreen
import com.divyang.studymateai.utils.AuthEvent
import com.divyang.studymateai.utils.AuthEventBus
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun StudyMateNavHost(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier
) {

    val context = LocalContext.current
    val sharedPref = remember { SharedPref(context) }
    val auth = remember { Firebase.auth }

    // Trust the live Firebase session, not just the cached flag. A stale
    // IS_LOGGED_IN pref with no live user (session revoked/expired) must not
    // land the user on a protected screen — clear it and start at Login.
    val hasLiveSession = auth.currentUser != null
    if (sharedPref.isLoggedIn() && !hasLiveSession) {
        sharedPref.clearUserSession()
    }
    val startDestination = Routes.getStartDestination(sharedPref.isLoggedIn() && hasLiveSession)

    LaunchedEffect(Unit) {
        AuthEventBus.events.collect { event ->
            if (event is AuthEvent.SessionExpired) {
                navController.navigate(Routes.Login.route) {
                    popUpTo(0)
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { screenSlideIn() },
        exitTransition = { screenFadeOut() },
        popEnterTransition = { screenFadeIn() },
        popExitTransition = { screenSlideOut() },
    ) {
        composable(Routes.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                },
                onSignUpClick = { navController.navigate(Routes.SignUp.route) },
                onForgotPasswordClick = {
                    navController.navigate(Routes.ForgetPassword.route)
                }
            )
        }
        composable(Routes.ForgetPassword.route) {
            ForgotPasswordScreen(navController)
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


        composable(Routes.Home.route) { entry ->
            HomeScreen(navController, savedStateHandle = entry.savedStateHandle)
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
                navController = navController
            )
        }


        composable(
            route = Routes.Scan.fullRoute,
            arguments = listOf(
                navArgument(Routes.Scan.ARG_FROM_CAMERA) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(Routes.Scan.ARG_RETURN_RESULT) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val fromCamera = backStackEntry.arguments?.getBoolean(Routes.Scan.ARG_FROM_CAMERA) ?: false
            val returnResult = backStackEntry.arguments?.getBoolean(Routes.Scan.ARG_RETURN_RESULT) ?: false

            ScanScreen(
                navController = navController,
                fromCamera = fromCamera,
                returnResult = returnResult
            )
        }

        composable(Routes.ChapterView.route) { backStackEntry ->
            val chapterId =
                backStackEntry.arguments?.getString(Routes.ChapterView.ARG_CHAPTER_ID) ?: ""
//            ChapterViewScreen(chapterId, navController)
        }


        composable(
            route = Routes.TextEdit.route,
            arguments = listOf(
                navArgument(Routes.TextEdit.ARG_CHAPTER_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            TextEditorScreen(
                chapterId = backStackEntry.arguments?.getString(Routes.TextEdit.ARG_CHAPTER_ID),
                navController = navController,
                savedStateHandle = backStackEntry.savedStateHandle
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
            ChapterDetailScreen(navController, chapterId, savedStateHandle = backStackEntry.savedStateHandle)
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

        composable(
            route = Routes.Flashcards.route,
            arguments = listOf(
                navArgument(Routes.Summary.CHAPTER_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString(Routes.Summary.CHAPTER_ID) ?: ""
            FlashCardScreen(
                navController = navController,
                chapterId = chapterId
            )
        }


        composable(Routes.EditProfile.route) {
            EditProfileScreen(navController)
        }

        composable(Routes.ChangeEmail.route) {
            ChangeEmailScreen(navController)
        }

        composable(Routes.ChangePassword.route) {
            ChangePasswordScreen(navController)
        }

        composable(Routes.AppSettings.route) {
            AppSettingsScreen(navController)
        }

        composable(Routes.PrivacyPolicy.route) {
            PrivacyPolicyScreen(navController)
        }


    }
}