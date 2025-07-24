package com.example.studymateai.ui.screen.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.studymateai.data.viewmodel.EditProfileViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userState by viewModel.userState.collectAsState()
    val firstNameState = remember { mutableStateOf(userState.firstName) }
    val lastNameState = remember { mutableStateOf(userState.lastName) }
    val isUpdating = viewModel.updateState.collectAsState().value

    // Handle toast messages
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Update local state when userState changes
    LaunchedEffect(userState) {
        firstNameState.value = userState.firstName
        lastNameState.value = userState.lastName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        viewModel.updateProfile(
                            firstName = firstNameState.value,
                            lastName = lastNameState.value
                        )
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Save") },
                text = { Text("Save Changes") },
                expanded = !isUpdating,
                loading = isUpdating
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (userState.uid.isEmpty()) {
                // Show loading indicator while profile is loading
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Rest of your UI...
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.CenterHorizontally)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${userState.firstName.firstOrNull()}${userState.lastName.firstOrNull()}",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = userState.email,
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.Email, contentDescription = "Email")
                    }
                )

                OutlinedTextField(
                    value = firstNameState.value,
                    onValueChange = { firstNameState.value = it },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = firstNameState.value.isBlank(),
                    supportingText = {
                        if (firstNameState.value.isBlank()) {
                            Text("First name cannot be empty")
                        }
                    }
                )

                OutlinedTextField(
                    value = lastNameState.value,
                    onValueChange = { lastNameState.value = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = lastNameState.value.isBlank(),
                    supportingText = {
                        if (lastNameState.value.isBlank()) {
                            Text("Last name cannot be empty")
                        }
                    }
                )

                Text(
                    text = "Member since ${userState.createdAt?.toDate()?.formatTo("MMMM dd, yyyy")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}



@Composable
fun ExtendedFloatingActionButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    expanded: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            icon()
        }

        if (expanded) {
            Spacer(modifier = Modifier.width(8.dp))
            text()
        }
    }
}

fun Date.formatTo(pattern: String): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(this)
}