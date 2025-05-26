package com.example.studymateai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.studymateai.navigation.Routes
import com.example.studymateai.navigation.StudyMateNavHost
import com.example.studymateai.shredPrefs.SharedPref
import com.example.studymateai.ui.components.BottomNavigationBar
import com.example.studymateai.ui.theme.StudyMateAITheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.initialize


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        Firebase.initialize(this)

        setContent {
            StudyMateAITheme {
                val navController = rememberNavController()
                val auth = Firebase.auth
                val sharedPref = remember { SharedPref(this) }

                val navBackStackEntry by navController.currentBackStackEntryAsState()

                val currentRoute = navBackStackEntry?.destination?.route
                // Check auth state
                val startDestination = remember {
                    if (auth.currentUser != null || sharedPref.isLoggedIn()) {
                        Routes.Home.route
                    } else {
                        Routes.Login.route
                    }
                }

                Scaffold(
                    bottomBar = {
                        if (currentRoute in listOf(
                                Routes.Home.route,
                                Routes.Library.route,
                                Routes.History.route,
                                Routes.Profile.route
                            )) {
                            BottomNavigationBar(navController)
                        }
                    }
                ) { innerPadding ->
                    StudyMateNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

