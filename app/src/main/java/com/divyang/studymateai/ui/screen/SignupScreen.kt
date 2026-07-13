package com.divyang.studymateai.ui.screen


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.divyang.studymateai.data.viewmodel.SignUpViewModel
import com.divyang.studymateai.ui.components.AppColors
import com.divyang.studymateai.ui.components.AuthScreenScaffold
import com.divyang.studymateai.ui.components.AuthTextField
import com.divyang.studymateai.ui.components.PrimaryButton

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onLoginClick: () -> Unit,
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.generalError) {
        uiState.generalError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearGeneralError()
        }
    }

    AuthScreenScaffold(
        title = "Create account",
        subtitle = "Start your study journey",
        snackbarHostState = snackbarHostState
    ) {
        // First + Last name row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AuthTextField(
                value = uiState.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = "First name",
                modifier = Modifier.weight(1f),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                isError = uiState.firstNameError != null,
                errorText = uiState.firstNameError
            )
            AuthTextField(
                value = uiState.lastName,
                onValueChange = viewModel::onLastNameChange,
                label = "Last name",
                modifier = Modifier.weight(1f),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                isError = uiState.lastNameError != null,
                errorText = uiState.lastNameError
            )
        }

        Spacer(Modifier.height(12.dp))

        AuthTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email",
            keyboardType = KeyboardType.Email,
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            isError = uiState.emailError != null,
            errorText = uiState.emailError
        )

        Spacer(Modifier.height(12.dp))

        AuthTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            isPassword = true,
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            isError = uiState.passwordError != null,
            errorText = uiState.passwordError
        )

        Spacer(Modifier.height(12.dp))

        AuthTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = "Confirm password",
            isPassword = true,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); viewModel.signUp() }),
            isError = uiState.confirmPasswordError != null,
            errorText = uiState.confirmPasswordError
        )

        Spacer(Modifier.height(12.dp))

        // No account without explicit agreement: sign-up is blocked until the
        // box is checked, and acceptance is recorded on the user's profile.
        val linkStyle = TextLinkStyles(
            style = SpanStyle(color = AppColors.Purple, fontWeight = FontWeight.Medium)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.termsAgreed,
                onCheckedChange = viewModel::onTermsAgreedChange,
                colors = CheckboxDefaults.colors(checkedColor = AppColors.Purple)
            )
            Text(
                text = buildAnnotatedString {
                    append("I agree to the ")
                    withLink(LinkAnnotation.Clickable("terms", linkStyle) { onTermsClick() }) {
                        append("Terms & Conditions")
                    }
                    append(" and ")
                    withLink(LinkAnnotation.Clickable("privacy", linkStyle) { onPrivacyClick() }) {
                        append("Privacy Policy")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        uiState.termsError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        PrimaryButton(
            text = "Create account",
            onClick = { focusManager.clearFocus(); viewModel.signUp() },
            isLoading = uiState.isLoading
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onLoginClick) { Text("Login") }
        }
    }

    // Verification dialog
    if (uiState.showVerificationDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Check your email") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account created successfully!")
                    Text(
                        "We sent a verification link to ${uiState.signUpSuccessEmail}. " +
                                "Please verify before logging in.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissVerificationDialog(); onLoginClick() },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Go to login") }
            }
        )
    }
}
