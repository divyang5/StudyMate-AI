package com.divyang.studymateai.ui.screen.profile


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.divyang.studymateai.data.viewmodel.ChangePasswordViewModel
import com.divyang.studymateai.ui.components.AppTopBar
import com.divyang.studymateai.ui.components.AuthTextField
import com.divyang.studymateai.ui.components.PrimaryButton
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController,
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val newTooShort = uiState.newPassword.isNotEmpty() && uiState.newPassword.length < 8
    val passwordsMatch = uiState.newPassword.isEmpty() || uiState.newPassword == uiState.confirmPassword

    Scaffold(
        topBar = { AppTopBar(title = "Change password", onBack = { navController.popBackStack() }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Enter your current password and choose a new one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            AuthTextField(
                value = uiState.currentPassword,
                onValueChange = viewModel::updateCurrentPassword,
                label = "Current password",
                isPassword = true,
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            AuthTextField(
                value = uiState.newPassword,
                onValueChange = viewModel::updateNewPassword,
                label = "New password",
                isPassword = true,
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                isError = newTooShort,
                errorText = if (newTooShort) "At least 8 characters" else null
            )

            AuthTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::updateConfirmPassword,
                label = "Confirm new password",
                isPassword = true,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); viewModel.changePassword() }),
                isError = !passwordsMatch,
                errorText = if (!passwordsMatch) "Passwords don't match" else null
            )

            Spacer(Modifier.height(16.dp))

            PrimaryButton(
                text = "Update password",
                onClick = { focusManager.clearFocus(); viewModel.changePassword() },
                isLoading = uiState.isLoading,
                enabled = uiState.currentPassword.isNotEmpty() &&
                        uiState.newPassword.length >= 8 &&
                        uiState.newPassword == uiState.confirmPassword
            )
        }
    }
}
