package com.example.studymateai.ui.screen.chapter


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.studymateai.data.viewmodel.TextEditorViewModel
import com.example.studymateai.navigation.Routes
import java.net.URLEncoder


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    // Decoded strings received from nav args
    title: String = "",
    description: String = "",
    extractedText: String = "",
    chapterId: String? = null,           // null = create, non-null = edit existing
    navController: NavController,
    viewModel: TextEditorViewModel = viewModel()
) {
    // Seed ViewModel once on first composition
    LaunchedEffect(Unit) {
        viewModel.init(
            title = title,
            description = description,
            content = extractedText,
            chapterId = chapterId        // tells ViewModel whether to update or add
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    // Success → navigate home
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.clearSuccess()
            navController.navigate("home") {
                popUpTo("textEditor") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                        Text(
                            text = ("Edit Chapter"),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Content field — largest, at top
            SectionLabel("Content")
            OutlinedTextField(
                value = uiState.content,
                onValueChange = viewModel::onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 320.dp),
                label = { Text("Chapter text") },
                isError = uiState.showValidationErrors && uiState.content.isBlank(),
                supportingText = if (uiState.showValidationErrors && uiState.content.isBlank()) {
                    { Text("Content is required") }
                } else null,
                shape = RoundedCornerShape(14.dp),
                maxLines = 20
            )

            // Title
            SectionLabel("Title")
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Chapter title") },
                isError = uiState.showValidationErrors && uiState.title.isBlank(),
                supportingText = if (uiState.showValidationErrors && uiState.title.isBlank()) {
                    { Text("Title is required") }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Description
            SectionLabel("Description")
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("Short description") },
                isError = uiState.showValidationErrors && uiState.description.isBlank(),
                supportingText = if (uiState.showValidationErrors && uiState.description.isBlank()) {
                    { Text("Description is required") }
                } else null,
                shape = RoundedCornerShape(14.dp),
                maxLines = 5
            )

            Spacer(Modifier.height(4.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val encodedText = URLEncoder.encode(uiState.content, "UTF-8")
                        navController.navigate(
                            Routes.Scan.createRoute(fromCamera = false, existingText = encodedText)
                        ) {
                            popUpTo("textEditor") { inclusive = true }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("Scan More")
                }

                Button(
                    onClick = { viewModel.save() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // Error snackbar via AlertDialog
    uiState.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { viewModel.clearError() }) { Text("OK") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}