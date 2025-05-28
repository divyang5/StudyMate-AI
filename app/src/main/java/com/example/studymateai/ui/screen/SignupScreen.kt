package com.example.studymateai.ui.screen

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.studymateai.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onLoginClick: () -> Unit,
) {
    val firstNameState = remember { mutableStateOf("") }
    val lastNameState = remember { mutableStateOf("") }
    val emailState = remember { mutableStateOf("") }
    val passwordState = remember { mutableStateOf("") }
    val confirmPasswordState = remember { mutableStateOf("") }
    val passwordErrorState = remember { mutableStateOf<String?>(null) }
    val isLoading = remember { mutableStateOf(false) }
    val showSuccessDialog = remember { mutableStateOf(false) }
    val showErrorDialog = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf("") }

    val auth: FirebaseAuth = Firebase.auth
    val firestore = Firebase.firestore

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo/Title
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "StudyMate AI Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Signup",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Name Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = firstNameState.value,
                    onValueChange = { firstNameState.value = it },
                    label = { Text("First Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = lastNameState.value,
                    onValueChange = { lastNameState.value = it },
                    label = { Text("Last Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Email Field
            OutlinedTextField(
                value = emailState.value,
                onValueChange = { emailState.value = it },
                label = { Text("Email") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = passwordState.value,
                onValueChange = { passwordState.value = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = passwordErrorState.value != null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPasswordState.value,
                onValueChange = { confirmPasswordState.value = it },
                label = { Text("Re-Enter Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = passwordErrorState.value != null,
                supportingText = {
                    if (passwordErrorState.value != null) {
                        Text(text = passwordErrorState.value!!)
                    }
                }
            )


            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up Button
            Button(
                onClick = {
                    if (firstNameState.value.isEmpty() || lastNameState.value.isEmpty()) {
                        errorMessage.value = "Please enter your name"
                        showErrorDialog.value = true
                        return@Button
                    }

                    val error = validatePassword(passwordState.value, confirmPasswordState.value)
                    passwordErrorState.value = error
                    if (error == null) {
                        isLoading.value = true
                        createUserWithEmailAndPassword(
                            auth = auth,
                            email = emailState.value,
                            password = passwordState.value,
                            firstName = firstNameState.value,
                            lastName = lastNameState.value,
                            onSuccess = {
                                isLoading.value = false
                                showSuccessDialog.value = true
                            },
                            onError = { message ->
                                isLoading.value = false
                                errorMessage.value = message
                                showErrorDialog.value = true
                            },
                            firestore = firestore
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isLoading.value
            ) {
                if (isLoading.value) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Signup")
                }
            }


            Spacer(modifier = Modifier.height(24.dp))

            // Already have account - Login
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account?")
                TextButton(onClick = onLoginClick) {
                    Text("Login Now!")
                }
            }
        }

        // Success Dialog
        if (showSuccessDialog.value) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog.value = false },
                title = { Text("Account Created") },
                text = { Text("Your account has been created successfully!") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuccessDialog.value = false
                            onSignUpSuccess()
                        }
                    ) {
                        Text("Continue")
                    }
                }
            )
        }

        // Error Dialog
        if (showErrorDialog.value) {
            AlertDialog(
                onDismissRequest = { showErrorDialog.value = false },
                title = { Text("Sign Up Failed") },
                text = { Text(errorMessage.value) },
                confirmButton = {
                    Button(
                        onClick = { showErrorDialog.value = false }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

// Updated Firebase functions
private suspend fun createUserWithFirebase(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    email: String,
    password: String,
    firstName: String,
    lastName: String
): Result<Unit> {
    return try {
        // 1. Create auth user
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()

        // 2. Create user document in Firestore
        val user = hashMapOf(
            "uid" to authResult.user?.uid,
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(authResult.user?.uid ?: "")
            .set(user)
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun createUserWithEmailAndPassword(
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    email: String,
    password: String,
    firstName: String,
    lastName: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { authTask ->
            if (authTask.isSuccessful) {
                // Create user document in Firestore
                val user = hashMapOf(
                    "uid" to authTask.result.user?.uid,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to email,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                firestore.collection("users")
                    .document(authTask.result.user?.uid ?: "")
                    .set(user)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onError(e.message ?: "Failed to create user profile") }
            } else {
                onError(authTask.exception?.message ?: "Unknown error occurred")
            }
        }
}

// Password validation function
fun validatePassword(password: String, confirmPassword: String): String? {
    if (password.length < 8) {
        return "Password must be at least 8 characters"
    }

    if (!password.any { it.isDigit() }) {
        return "Password must contain at least one digit"
    }

    if (!password.any { it.isLetter() }) {
        return "Password must contain at least one letter"
    }

    if (password != confirmPassword) {
        return "Passwords don't match"
    }

    return null
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SignUpScreenPreview() {
    MaterialTheme {
        SignUpScreen(
            onSignUpSuccess = {},
            onLoginClick = {},
        )
    }
}