package com.divyang.studymateai.ui.screen


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.divyang.studymateai.data.viewmodel.LoginViewModel
import com.divyang.studymateai.shredPrefs.SharedPref
import com.divyang.studymateai.ui.components.AuthScreenScaffold
import com.divyang.studymateai.ui.components.AuthTextField
import com.divyang.studymateai.ui.components.PrimaryButton
import com.google.firebase.auth.ktx.auth

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        val sharedPref = SharedPref(context)
        val previousEmail = sharedPref.getEmail()
        val firebaseUser = com.google.firebase.ktx.Firebase.auth.currentUser
        if (sharedPref.isLoggedIn() && !previousEmail.isNullOrBlank() && firebaseUser == null) {
            viewModel.prefillFromPreviousSession(previousEmail)
        }
    }

    // Save session + navigate on success
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            val sharedPref = SharedPref(context)
            val firebaseUser = com.google.firebase.ktx.Firebase.auth.currentUser
            val uid = firebaseUser?.uid

            // Only mark the session logged-in with a real uid. If the live user
            // is somehow null here, don't persist a blank-uid "logged in" state.
            if (uid.isNullOrBlank()) return@LaunchedEffect

            val displayName = firebaseUser.displayName.orEmpty()
            val parts = displayName.trim().split(" ")
            val firstName = parts.getOrNull(0).orEmpty()
            val lastName = parts.drop(1).joinToString(" ")
            val email = firebaseUser.email.orEmpty()

            sharedPref.saveUserSession(uid, firstName, lastName, email)

            onLoginSuccess()
        }
    }

    LaunchedEffect(uiState.generalError) {
        uiState.generalError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearGeneralError()
        }
    }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    AuthScreenScaffold(
        title = "Welcome back",
        subtitle = "Sign in to continue",
        snackbarHostState = snackbarHostState
    ) {
        AuthTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            isError = uiState.emailError != null,
            errorText = uiState.emailError
        )

        Spacer(Modifier.height(12.dp))

        AuthTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            isPassword = true,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus(); viewModel.login() }
            ),
            isError = uiState.passwordError != null,
            errorText = uiState.passwordError
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onForgotPasswordClick) {
                Text("Forgot password?")
            }
        }

        Spacer(Modifier.height(8.dp))

        PrimaryButton(
            text = "Login",
            onClick = { focusManager.clearFocus(); viewModel.login() },
            isLoading = uiState.isLoading
        )

        if (uiState.isEmailUnverified) {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { viewModel.resendVerificationEmail() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Resend Verification Email",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onSignUpClick) {
                Text("Sign up")
            }
        }
    }
}
