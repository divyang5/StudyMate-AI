package com.example.studymateai.ui.screen.chapter

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

@Composable
fun TextEditorScreen(
    extractedText: String,
    navController: NavController,
    context: Context = LocalContext.current
) {
    val titleState = remember { mutableStateOf("") }
    val descriptionState = remember { mutableStateOf("") }
    val editableTextState = remember { mutableStateOf(extractedText) }
    val showError = remember { mutableStateOf(false) }
    val showSuccess = remember { mutableStateOf(false) }
    val isSaving = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Editable Extracted Text Section
        Text(
            text = "Edit Extracted Text",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = editableTextState.value,
            onValueChange = { editableTextState.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            label = { Text("Extracted Text*") },
            isError = showError.value && editableTextState.value.isEmpty()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title Input
        OutlinedTextField(
            value = titleState.value,
            onValueChange = { titleState.value = it },
            label = { Text("Title*") },
            modifier = Modifier.fillMaxWidth(),
            isError = showError.value && titleState.value.isEmpty()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description Input
        OutlinedTextField(
            value = descriptionState.value,
            onValueChange = { descriptionState.value = it },
            label = { Text("Description*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            isError = showError.value && descriptionState.value.isEmpty()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    navController.navigate("scan?fromCamera=false") {
                        popUpTo("textEditor") { inclusive = true }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Scan More Text")
            }

            Button(
                onClick = {
                    if (
                        titleState.value.isEmpty() ||
                        descriptionState.value.isEmpty() ||
                        editableTextState.value.isEmpty()
                    ) {
                        showError.value = true
                    } else {
                        isSaving.value = true
                        saveToFirestore(
                            title = titleState.value,
                            description = descriptionState.value,
                            extractedText = editableTextState.value,
                            onSuccess = {
                                isSaving.value = false
                                showSuccess.value = true
                            },
                            onError = {
                                isSaving.value = false
                                showError.value = true
                            }
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isSaving.value
            ) {
                if (isSaving.value) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text("Save")
                }
            }
        }
    }

    // Error Dialog
    if (showError.value) {
        AlertDialog(
            onDismissRequest = { showError.value = false },
            title = { Text("Error") },
            text = {
                Text(
                    when {
                        titleState.value.isEmpty() && descriptionState.value.isEmpty() && editableTextState.value.isEmpty() ->
                            "Please enter title, description and extracted text"
                        titleState.value.isEmpty() -> "Please enter a title"
                        descriptionState.value.isEmpty() -> "Please enter a description"
                        editableTextState.value.isEmpty() -> "Extracted text cannot be empty"
                        else -> "Failed to save. Please try again."
                    }
                )
            },
            confirmButton = {
                Button(onClick = { showError.value = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Success Dialog
    if (showSuccess.value) {
        AlertDialog(
            onDismissRequest = { showSuccess.value = false },
            title = { Text("Success") },
            text = { Text("Document saved successfully!") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccess.value = false
                        navController.navigate("home") {
                            popUpTo("textEditor") { inclusive = true }
                        }
                    }
                ) {
                    Text("Go to Home")
                }
            }
        )
    }
}

private fun saveToFirestore(
    title: String,
    description: String,
    extractedText: String,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val db = Firebase.firestore
    val userId = Firebase.auth.currentUser?.uid ?: return onError()

    val documentData = hashMapOf(
        "title" to title,
        "description" to description,
        "content" to extractedText,
        "createdAt" to FieldValue.serverTimestamp(),
        "userId" to userId
    )

    db.collection("chapters")
        .add(documentData)
        .addOnSuccessListener {
            onSuccess()
        }
        .addOnFailureListener {
            onError()
        }
}