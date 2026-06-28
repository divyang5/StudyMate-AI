package com.divyang.studymateai.ui.screen.chapter


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.divyang.studymateai.data.viewmodel.TextEditorViewModel
import com.divyang.studymateai.navigation.Routes
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    title: String = "",
    description: String = "",
    extractedText: String = "",
    chapterId: String? = null,
    navController: NavController,
    viewModel: TextEditorViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.init(
            title = title,
            description = description,
            content = extractedText,
            chapterId = chapterId
        )
    }

    val uiState by viewModel.uiState.collectAsState()

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
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (chapterId != null) "Edit Chapter" else "New Chapter",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Surface(shape = CircleShape, color = Color(0xFFEEEDFE)) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF534AB7),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(6.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF534AB7), Color(0xFF1D9E75))
                            )
                        )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            EditorSectionLabel("Content")
            OutlinedTextField(
                value = uiState.content,
                onValueChange = viewModel::onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 320.dp),
                placeholder = { Text("Chapter text", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) },
                isError = uiState.showValidationErrors && uiState.content.isBlank(),
                supportingText = if (uiState.showValidationErrors && uiState.content.isBlank()) {
                    { Text("Content is required") }
                } else null,
                shape = RoundedCornerShape(14.dp),
                maxLines = 20,
                colors = outlinedFieldColors()
            )

            Spacer(Modifier.height(4.dp))
            EditorSectionLabel("Title")
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Chapter title", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) },
                isError = uiState.showValidationErrors && uiState.title.isBlank(),
                supportingText = if (uiState.showValidationErrors && uiState.title.isBlank()) {
                    { Text("Title is required") }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = outlinedFieldColors()
            )

            Spacer(Modifier.height(4.dp))
            EditorSectionLabel("Description")
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = { Text("Short description", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) },
                isError = uiState.showValidationErrors && uiState.description.isBlank(),
                supportingText = if (uiState.showValidationErrors && uiState.description.isBlank()) {
                    { Text("Description is required") }
                } else null,
                shape = RoundedCornerShape(14.dp),
                maxLines = 5,
                colors = outlinedFieldColors()
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val encodedText = URLEncoder.encode(uiState.content, "UTF-8")
                        navController.navigate(
                            Routes.Scan.createRoute(fromCamera = false, existingText = encodedText)
                        ) { popUpTo("textEditor") { inclusive = true } }
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.5.dp, Color(0xFF534AB7)),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("Scan More", color = Color(0xFF534AB7))
                }

                Button(
                    onClick = { viewModel.save() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF534AB7),
                        contentColor   = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = !uiState.isSaving,
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Save", fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    uiState.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text  = { Text(msg) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearError() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF534AB7))
                ) { Text("OK") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun EditorSectionLabel(text: String) {
    Text(
        text     = text.uppercase(),
        style    = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
    )
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Color(0xFF534AB7),
    unfocusedBorderColor = Color(0xFF534AB7).copy(alpha = 0.25f),
    focusedLabelColor    = Color(0xFF534AB7),
    cursorColor          = Color(0xFF534AB7)
)