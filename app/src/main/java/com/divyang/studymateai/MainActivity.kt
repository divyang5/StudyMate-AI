package com.divyang.studymateai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.divyang.studymateai.navigation.Routes
import com.divyang.studymateai.navigation.StudyMateNavHost
import com.divyang.studymateai.ui.components.BottomNavigationBar
import com.divyang.studymateai.ui.theme.StudyMateAITheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContent {
            StudyMateAITheme {
                val navController = rememberNavController()

                val navBackStackEntry by navController.currentBackStackEntryAsState()

                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.imePadding(),
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
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

