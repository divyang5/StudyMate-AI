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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.divyang.studymateai.R
import com.divyang.studymateai.data.viewmodel.ProfileViewModel
import com.divyang.studymateai.navigation.Routes
import com.divyang.studymateai.ui.components.BottomNavigationBar

private const val DELETE_CONFIRMATION_WORD = "DELETE"

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

    // Danger zone dialog state
    var showDeleteWarningDialog by rememberSaveable { mutableStateOf(false) }
    var showFinalDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var deleteConfirmationInput by rememberSaveable { mutableStateOf("") }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.logoutSuccess) {
        if (uiState.logoutSuccess) onLogout()
    }
    // Reuses the exact same navigation as a normal logout — sign out is
    // already done in the ViewModel, this just moves the user to the login screen.
    LaunchedEffect(uiState.accountDeleted) {
        if (uiState.accountDeleted) onLogout()
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

            // ── Danger Zone ────────────────────────────────────
            ProfileSectionLabel("Danger Zone")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Permanently delete your account and all associated study data. This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showDeleteWarningDialog = true },
                        enabled = !uiState.isDeletingAccount,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFA32D2D)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isDeletingAccount) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFA32D2D)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Deleting…")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Delete Account")
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Step 1 — explain the consequences
    if (showDeleteWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteWarningDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFA32D2D)
                )
            },
            title = { Text("Delete your account?") },
            text = {
                Text(
                    "This will permanently delete your profile, study materials, chat history, " +
                            "and all other data linked to your account from our servers. This action " +
                            "cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteWarningDialog = false
                        deleteConfirmationInput = ""
                        showFinalDeleteDialog = true
                    }
                ) {
                    Text("Continue", color = Color(0xFFA32D2D))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Step 2 — type DELETE to confirm intent
    if (showFinalDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showFinalDeleteDialog = false
                deleteConfirmationInput = ""
            },
            title = { Text("Are you absolutely sure?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Type \"$DELETE_CONFIRMATION_WORD\" to confirm. This is your last chance to back out.")
                    OutlinedTextField(
                        value = deleteConfirmationInput,
                        onValueChange = { deleteConfirmationInput = it },
                        label = { Text(DELETE_CONFIRMATION_WORD) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFinalDeleteDialog = false
                        passwordInput = ""
                        showPasswordDialog = true
                    },
                    enabled = deleteConfirmationInput == DELETE_CONFIRMATION_WORD
                ) {
                    Text(
                        "Continue",
                        color = if (deleteConfirmationInput == DELETE_CONFIRMATION_WORD) {
                            Color(0xFFA32D2D)
                        } else {
                            Color(0xFFA32D2D).copy(alpha = 0.4f)
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showFinalDeleteDialog = false
                        deleteConfirmationInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Step 3 — password required, verified BEFORE anything is deleted.
    // Dialog stays open on wrong password so the user can just retry.
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isDeletingAccount) {
                    showPasswordDialog = false
                    passwordInput = ""
                    isPasswordVisible = false
                }
            },
            title = { Text("Confirm your password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("For your security, enter your password to permanently delete your account.")
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        singleLine = true,
                        enabled = !uiState.isDeletingAccount,
                        visualTransformation = if (isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(
                                onClick = { isPasswordVisible = !isPasswordVisible },
                                enabled = !uiState.isDeletingAccount
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (isPasswordVisible) R.drawable.visibility_on
                                        else R.drawable.visibility_off
                                    ),
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteAccount(passwordInput) },
                    enabled = passwordInput.isNotBlank() && !uiState.isDeletingAccount
                ) {
                    if (uiState.isDeletingAccount) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Delete Permanently", color = Color(0xFFA32D2D))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPasswordDialog = false
                        passwordInput = ""
                        isPasswordVisible = false
                    },
                    enabled = !uiState.isDeletingAccount
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Once the account is actually deleted, close the dialog so it doesn't
    // briefly reappear while navigation to the login screen happens.
    LaunchedEffect(uiState.accountDeleted) {
        if (uiState.accountDeleted) {
            showPasswordDialog = false
            passwordInput = ""
            isPasswordVisible = false
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