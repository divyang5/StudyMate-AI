package com.divyang.studymateai.ui.screen.profile

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.divyang.studymateai.data.viewmodel.GeminiKeyViewModel
import com.divyang.studymateai.gemini.GenerationQuota
import com.divyang.studymateai.ui.components.AppColors
import com.divyang.studymateai.ui.components.AppTopBar
import com.divyang.studymateai.ui.components.ConfirmationDialog

private const val AI_STUDIO_URL = "https://aistudio.google.com/app/apikey"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiKeySettingsScreen(
    navController: NavController,
    viewModel: GeminiKeyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showRemoveConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { AppTopBar(title = "Gemini API key", onBack = { navController.popBackStack() }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // ── Current status ──────────────────────────────────────────
            item {
                KeyCard {
                    if (uiState.hasPersonalKey) {
                        Text(
                            "Your key is active",
                            style = MaterialTheme.typography.titleSmall,
                            color = AppColors.TealDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Generations are unlimited and use your own Gemini quota. " +
                                "Your key is stored encrypted on this device only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showRemoveConfirm = true }) {
                            Text("Remove key", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Text(
                            "Free plan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${uiState.remainingToday} of ${GenerationQuota.DAILY_FREE_LIMIT} free " +
                                "generations left today (quizzes, summaries, flashcards). " +
                                "The limit resets daily — or add your own free key below for " +
                                "unlimited generations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── How to get a key ────────────────────────────────────────
            item {
                KeyCard {
                    Text(
                        "How to get a free key",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Google doesn't let apps create the key for you, so it's a quick " +
                            "one-time copy-paste:\n\n" +
                            "1. Tap the button below to open Google AI Studio\n" +
                            "2. Sign in with your Google account\n" +
                            "3. Tap “Create API key”\n" +
                            "4. Copy the key and paste it here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, AI_STUDIO_URL.toUri()))
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, AppColors.Purple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Google AI Studio", color = AppColors.Purple)
                    }
                }
            }

            // ── Paste + validate ────────────────────────────────────────
            item {
                KeyCard {
                    Text(
                        if (uiState.hasPersonalKey) "Replace your key" else "Paste your key",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.keyInput,
                        onValueChange = viewModel::onKeyInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Paste your key (e.g. AIza… or AQ.…)",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                        },
                        isError = uiState.errorMessage != null,
                        supportingText = uiState.errorMessage?.let { msg -> { Text(msg) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Purple,
                            unfocusedBorderColor = AppColors.Purple.copy(alpha = 0.25f),
                            cursorColor = AppColors.Purple
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.validateAndSave() },
                        enabled = !uiState.isValidating && viewModel.keyInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Purple,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        if (uiState.isValidating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Text("Verifying key…")
                            }
                        } else {
                            Text("Validate & Save", fontWeight = FontWeight.Medium)
                        }
                    }
                    if (uiState.keyJustSaved) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Key verified and saved — generations are now unlimited.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TealDark
                        )
                    }
                }
            }

            // ── What it means ───────────────────────────────────────────
            item {
                KeyCard {
                    Text(
                        "Good to know",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "• Your key never leaves this device and is stored encrypted\n" +
                            "• Generation requests go directly to Google using your key, so " +
                            "usage counts against your own (free) Gemini quota\n" +
                            "• You can remove the key anytime to go back to the free plan\n" +
                            "• Signing out of the app removes the key from this device " +
                            "for your security — re-enter it on your next login",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showRemoveConfirm) {
        ConfirmationDialog(
            title = "Remove your key?",
            message = "You'll go back to the free plan " +
                "(${GenerationQuota.DAILY_FREE_LIMIT} generations per day with the app's shared key).",
            confirmText = "Remove",
            onConfirm = {
                showRemoveConfirm = false
                viewModel.removeKey()
            },
            onDismiss = { showRemoveConfirm = false }
        )
    }
}

@Composable
private fun KeyCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
