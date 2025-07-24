package com.example.studymateai.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    onBackClick: () -> Unit = { navController.popBackStack() }
) {
    val emailState = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val messageState = remember { mutableStateOf<String?>(null) }
    val showDialog = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth: FirebaseAuth = Firebase.auth

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Enter your email address to receive a password reset link",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = emailState.value,
                onValueChange = { emailState.value = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = messageState.value != null
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (emailState.value.isBlank()) {
                        messageState.value = "Please enter your email"
                        showDialog.value = true
                    } else {
                        isLoading.value = true
                        sendPasswordResetEmail(
                            auth = auth,
                            email = emailState.value,
                            onSuccess = {
                                isLoading.value = false
                                messageState.value = "Password reset email sent to ${emailState.value}"
                                showDialog.value = true
                            },
                            onError = { error ->
                                isLoading.value = false
                                messageState.value = error
                                showDialog.value = true
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isLoading.value
            ) {
                if (isLoading.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Send Reset Link")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onBackClick
            ) {
                Text("Back to Login")
            }
        }

        // Result Dialog
        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = {
                    showDialog.value = false
                    if (messageState.value?.contains("sent") == true) {
                        onBackClick()
                    }
                },
                title = {
                    Text(
                        if (messageState.value?.contains("sent") == true) "Email Sent"
                        else "Error"
                    )
                },
                text = { Text(messageState.value ?: "") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDialog.value = false
                            if (messageState.value?.contains("sent") == true) {
                                onBackClick()
                            }
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

private fun sendPasswordResetEmail(
    auth: FirebaseAuth,
    email: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    auth.sendPasswordResetEmail(email)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                val error = task.exception?.message ?: "Failed to send reset email"
                onError(error)
            }
        }
}

@Preview
@Composable
fun ForgotPasswordScreenPreview() {
    MaterialTheme {
        ForgotPasswordScreen(navController = rememberNavController())
    }
}