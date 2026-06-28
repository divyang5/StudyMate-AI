package com.divyang.studymateai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.divyang.studymateai.R
import com.divyang.studymateai.navigation.Routes

data class BottomNavItem(
    val route: String,
    val icon: Int,
    val label: String
)

@Composable
fun BottomNavigationBar(navController: NavController) {

    val items = listOf(
        BottomNavItem(Routes.Home.route, R.drawable.home, "Home"),
        BottomNavItem(Routes.Library.route, R.drawable.library, "Library"),
        BottomNavItem(Routes.History.route, R.drawable.history, "History"),
        BottomNavItem(Routes.Profile.route, R.drawable.profile, "Profile")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val itemPositions = remember { mutableStateMapOf<String, Pair<Float, Float>>() } // route → (x, width)
    val selectedRoute = items.firstOrNull { it.route == currentRoute }?.route
        ?: items.firstOrNull()?.route ?: return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Glass blur layer — needs RenderScript or accompanist blur
        // For pure Compose: use a layered background approach
        Box(
            modifier = Modifier
                .height(68.dp)
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(50.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.6f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(50.dp)
                )
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(50.dp),
                    ambientColor = Color.Black.copy(alpha = 0.15f),
                    spotColor = Color.Black.copy(alpha = 0.1f)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route

                    // Smooth scale animation
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.05f else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.6f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "scale"
                    )

                    // Animate pill width
                    val pillWidth by animateFloatAsState(
                        targetValue = if (selected) itemPositions[selectedRoute]?.second ?: 1.05f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness    = Spring.StiffnessMediumLow
                        ),
                        label = "pillWidth"
                    )

                    // Icon color animation
                    val iconColor by animateColorAsState(
                        targetValue = if (selected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else
                            Color.Black.copy(alpha = 0.5f),
                        animationSpec = tween(durationMillis = 0),
                        label = "iconColor"
                    )


                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = pillWidth
                            }
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected)
                                    Color.Transparent.copy(alpha = 0.05f)
                                else
                                    Color.Transparent

                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null // no ripple — iOS feel
                            ) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(item.icon),
                                contentDescription = item.label,
                                tint = iconColor,
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = iconColor
                            )
                        }
                    }
                }
            }
        }
    }
}