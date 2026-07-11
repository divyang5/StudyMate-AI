package com.divyang.studymateai.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.divyang.studymateai.ui.components.AuthScreenScaffold
import com.divyang.studymateai.ui.components.AuthTextField
import com.divyang.studymateai.ui.components.PrimaryButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    onBackClick: () -> Unit = { navController.popBackStack() }
) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val auth: FirebaseAuth = Firebase.auth

    AuthScreenScaffold(
        title = "Reset password",
        subtitle = "We'll email you a reset link"
    ) {
        Text(
            text = "Enter the email address associated with your account and we'll send you a link to reset your password.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            isError = showDialog && !isSuccess
        )

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            text = "Send reset link",
            isLoading = isLoading,
            onClick = {
                if (email.isBlank()) {
                    isSuccess = false
                    message = "Please enter your email"
                    showDialog = true
                } else {
                    isLoading = true
                    sendPasswordResetEmail(
                        auth = auth,
                        email = email,
                        onSuccess = {
                            isLoading = false
                            isSuccess = true
                            message = "Password reset email sent to $email"
                            showDialog = true
                        },
                        onError = { error ->
                            isLoading = false
                            isSuccess = false
                            message = error
                            showDialog = true
                        }
                    )
                }
            }
        )

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to login")
        }
    }

    // Result dialog
    if (showDialog) {
        val dismiss: () -> Unit = {
            showDialog = false
            if (isSuccess) onBackClick()
        }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(if (isSuccess) "Email sent" else "Error") },
            text = { Text(message ?: "") },
            confirmButton = {
                TextButton(onClick = dismiss) { Text("OK") }
            }
        )
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
