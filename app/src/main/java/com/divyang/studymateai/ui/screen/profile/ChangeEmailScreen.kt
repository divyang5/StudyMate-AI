package com.divyang.studymateai.ui.screen.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.divyang.studymateai.data.viewmodel.ChangeEmailViewModel
import com.divyang.studymateai.ui.components.AppTopBar
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeEmailScreen(
    navController: NavController,
    viewModel: ChangeEmailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val newEmailState = remember { mutableStateOf("") }
    val passwordState = remember { mutableStateOf("") }
    val isSendingVerification = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messageFlow.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            if (message.contains("success", ignoreCase = true)) {
                navController.popBackStack()
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Change Email", onBack = { navController.popBackStack() }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.uid.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column {
                    Text(
                        text = "Current Email:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.email,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (uiState.isEmailVerified) "Verified" else "Not Verified",
                            color = if (uiState.isEmailVerified) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }

                    if (!uiState.isEmailVerified) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                isSendingVerification.value = true
                                scope.launch {
                                    viewModel.sendVerificationEmail()
                                    isSendingVerification.value = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSendingVerification.value
                        ) {
                            if (isSendingVerification.value) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Send Verification Email")
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You must verify your current email before changing it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = "Enter New Email Address",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = newEmailState.value,
                    onValueChange = { newEmailState.value = it },
                    label = { Text("New Email") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = newEmailState.value.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmailState.value).matches(),
                    supportingText = {
                        if (newEmailState.value.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmailState.value).matches()) {
                            Text("Please enter a valid email address")
                        }
                    },
                    enabled = uiState.isEmailVerified
                )

                OutlinedTextField(
                    value = passwordState.value,
                    onValueChange = { passwordState.value = it },
                    label = { Text("Current Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = passwordState.value.isBlank(),
                    supportingText = {
                        if (passwordState.value.isBlank()) {
                            Text("Password is required")
                        }
                    },
                    enabled = uiState.isEmailVerified
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (android.util.Patterns.EMAIL_ADDRESS.matcher(newEmailState.value).matches()) {
                            viewModel.updateEmail(
                                newEmail = newEmailState.value,
                                password = passwordState.value
                            )
                        } else {
                            Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !uiState.isUpdating && uiState.isEmailVerified &&
                            newEmailState.value.isNotBlank() &&
                            passwordState.value.isNotBlank()
                ) {
                    if (uiState.isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Update Email")
                    }
                }
            }
        }
    }
}
