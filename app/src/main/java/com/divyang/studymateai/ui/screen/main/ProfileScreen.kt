package com.divyang.studymateai.ui.screen.main

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.divyang.studymateai.R
import com.divyang.studymateai.data.viewmodel.ProfileViewModel
import com.divyang.studymateai.navigation.Routes
import com.divyang.studymateai.ui.components.BottomNavigationBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    bottomPadding: PaddingValues = PaddingValues(0.dp),
    context: Context = LocalContext.current,
    navController: NavController,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(context))
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.logoutSuccess) {
        if (uiState.logoutSuccess) onLogout()
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(bottom = bottomPadding.calculateBottomPadding())
                .padding(horizontal = 16.dp)
        ) {

            // ── Accent strip ──────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .width(40.dp)
                    .height(3.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF534AB7), Color(0xFF1D9E75))
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )

            Spacer(Modifier.height(16.dp))

            // ── Avatar + Name + Email ─────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .background(Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = Color(0xFF534AB7),
                            shape = CircleShape
                        )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(88.dp)
                            .background(Color(0xFFEEEDFE), CircleShape)
                    ) {
                        Text(
                            text  = uiState.avatarInitials,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFF534AB7),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text  = uiState.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text  = uiState.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Pro badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFEEEDFE)
                ) {
                    Text(
                        text  = "✦ Pro Student",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF534AB7),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Account Section ───────────────────────────────
            ProfileSectionLabel("Account")
            ProfileCard {
                ProfileItem(
                    iconContent = { ProfileIconBox(color = Color(0xFFEEEDFE)) {
                        Icon(Icons.Default.Person, null, tint = Color(0xFF534AB7), modifier = Modifier.size(18.dp))
                    }},
                    text = "Edit Profile",
                    onClick = { navController.navigate(Routes.EditProfile.route) }
                )
                ProfileDivider()
                ProfileItem(
                    iconContent = { ProfileIconBox(color = Color(0xFFE1F5EE)) {
                        Icon(Icons.Default.Email, null, tint = Color(0xFF0F6E56), modifier = Modifier.size(18.dp))
                    }},
                    text = "Change Email",
                    onClick = { navController.navigate(Routes.ChangeEmail.route) }
                )
                ProfileDivider()
                ProfileItem(
                    iconContent = { ProfileIconBox(color = Color(0xFFFAEEDA)) {
                        Icon(Icons.Default.Lock, null, tint = Color(0xFF854F0B), modifier = Modifier.size(18.dp))
                    }},
                    text = "Change Password",
                    onClick = { navController.navigate(Routes.ChangePassword.route) }
                )
            }

            // ── Settings Section ──────────────────────────────
            ProfileSectionLabel("Settings")
            ProfileCard {
                ProfileItem(
                    iconContent = { ProfileIconBox(color = Color(0xFFEEEDFE)) {
                        Icon(Icons.Default.Settings, null, tint = Color(0xFF534AB7), modifier = Modifier.size(18.dp))
                    }},
                    text = "App Settings",
                    onClick = { navController.navigate(Routes.AppSettings.route) }
                )
                ProfileDivider()
                ProfileItem(
                    iconContent = { ProfileIconBox(color = Color(0xFFE1F5EE)) {
                        Icon(painterResource(R.drawable.privacy_tips), null, tint = Color(0xFF0F6E56), modifier = Modifier.size(18.dp))
                    }},
                    text = "Privacy Policy",
                    onClick = { navController.navigate(Routes.PrivacyPolicy.route) }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Logout Button ─────────────────────────────────
            Button(
                onClick  = { viewModel.logout() },
                enabled  = !uiState.isLoggingOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFCEBEB),
                    contentColor   = Color(0xFFA32D2D),
                    disabledContainerColor = Color(0xFFFCEBEB).copy(alpha = 0.6f)
                ),
                border = BorderStroke(0.5.dp, Color(0xFFA32D2D).copy(alpha = 0.3f))
            ) {
                if (uiState.isLoggingOut) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color(0xFFA32D2D),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text  = "Logout",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun ProfileSectionLabel(text: String) {
    Text(
        text     = text.uppercase(),
        style    = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border   = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(content = content)
    }
}

@Composable
private fun ProfileIconBox(color: Color, icon: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .background(color, RoundedCornerShape(10.dp))
    ) { icon() }
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    )
}

@Composable
fun ProfileItem(
    iconContent: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        iconContent()
        Text(
            text     = text,
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector       = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint              = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier          = Modifier.size(20.dp)
        )
    }
}