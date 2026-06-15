package com.example.studymateai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.studymateai.R
import com.example.studymateai.navigation.Routes

data class BottomNavItem(
    val route: String,
    val icon: Int,
    val label: String
)
@Composable
fun BottomNavigationBar(
    navController: NavController
) {

    val items = listOf(
        BottomNavItem(Routes.Home.route, R.drawable.home, "Home"),
        BottomNavItem(Routes.Library.route, R.drawable.library, "Library"),
        BottomNavItem(Routes.History.route, R.drawable.history, "History"),
        BottomNavItem(Routes.Profile.route, R.drawable.profile, "Profile")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route




    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {

        Surface(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .height(72.dp)
                .widthIn(min = 320.dp),

            shape = RoundedCornerShape(36.dp),

            color = Color.White.copy(alpha = 0.2f),
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.25f)
            ),


//            shadowElevation = 30.dp

        ) {

            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp),

                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                items.forEach { item ->

                    val selected = currentRoute == item.route
                    val scale = remember { Animatable(1f) }


                    LaunchedEffect(selected) {
                        if (selected) {
                            scale.animateTo(
                                1.25f,
                                animationSpec =spring(
                                    dampingRatio = 0.7f,
                                    stiffness = Spring.StiffnessVeryLow
                                )
                            )
                            scale.animateTo(
                                1f,
                                animationSpec =spring(
                                dampingRatio = 0.7f,
                                stiffness = Spring.StiffnessVeryLow
                                )
                            )
                        } else {
                            scale.animateTo(
                                1f,
                                animationSpec =spring(
                                    dampingRatio =0.75f,
                                    stiffness = Spring.StiffnessVeryLow,
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale.value
                                scaleY = scale.value
                            }
                            .height(60.dp)
                            .width(80.dp)
                            .clip(RoundedCornerShape( 36.dp))
                            .clickable {

                                navController.navigate(item.route) {

                                    popUpTo(
                                        navController.graph.findStartDestination().id
                                    ) {
                                        saveState = true
                                    }

                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .background(
                                if (selected)
                                     Color.Transparent.copy(alpha = 0.05f)
                                else
                                    Color.Transparent
                            )

//                            .padding(
//                                horizontal = 30.dp)
                            ,
                        contentAlignment = Alignment.Center
                        ,
                    ) {

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Icon(
                                imageVector = ImageVector.vectorResource(item.icon),
                                contentDescription = item.label,
                                tint =
                                    if (selected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Black.copy(alpha = 0.75f)
                            )

                        }
                    }
                }
            }
        }
    }
}