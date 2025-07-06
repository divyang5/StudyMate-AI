package com.example.studymateai.ui.screen.chapter

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.studymateai.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


enum class ScreenState {
    SCAN_SELECTION,
    SCANNING,
    EDITING
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    navController: NavController,
    initialText: String? = null,
    context: Context = LocalContext.current
) {

    val currentScreenState = remember { mutableStateOf(
        if (initialText.isNullOrEmpty()) ScreenState.SCAN_SELECTION else ScreenState.EDITING
    )}


    val showDiscardDialog = remember { mutableStateOf(false) }
    val extractedText = remember { mutableStateOf(initialText ?: "") }
    val titleState = remember { mutableStateOf("") }
    val descriptionState = remember { mutableStateOf("") }

    // UI states
    val isLoading = remember { mutableStateOf(false) }
    val showError = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val showSuccess = remember { mutableStateOf(false) }


    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val galleryPermissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )
    BackHandler(enabled = true) {
        when (currentScreenState.value) {
            ScreenState.SCAN_SELECTION -> {
                if (extractedText.value.isNotEmpty()) {
                    currentScreenState.value = ScreenState.EDITING
                } else {
                    navController.popBackStack()
                }
            }
            ScreenState.SCANNING -> {
                if (extractedText.value.isNotEmpty()) {
                    currentScreenState.value = ScreenState.EDITING
                } else {
                    currentScreenState.value = ScreenState.SCAN_SELECTION
                }
            }
            ScreenState.EDITING -> {
                if (extractedText.value.isNotEmpty()) {
                    showDiscardDialog.value = true
                } else {
                    navController.popBackStack()
                }
            }
        }
    }

    val selectedImageUri = remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri.value = it
            isLoading.value = true
            processUriForText(
                context = context,
                uri = it,
                onTextExtracted = { newText ->
                    extractedText.value = if (extractedText.value.isNotEmpty()) {
                        "${extractedText.value}\n\n$newText"
                    } else {
                        newText
                    }
                    isLoading.value = false
                    currentScreenState.value = ScreenState.EDITING
                },
                onError = {
                    isLoading.value = false
                    showError.value = true
                    errorMessage.value = "Failed to process image"
                }
            )
        }
    }

    // Check permissions when entering scan mode
    LaunchedEffect(currentScreenState.value) {
        if (currentScreenState.value == ScreenState.SCANNING && !cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Permission denied handling
    if ((currentScreenState.value == ScreenState.SCANNING && !cameraPermissionState.status.isGranted) ||
        (currentScreenState.value == ScreenState.SCAN_SELECTION && !galleryPermissionState.status.isGranted)) {
        PermissionDeniedDialog(
            isCamera = currentScreenState.value == ScreenState.SCANNING,
            onRequestAgain = {
                if (currentScreenState.value == ScreenState.SCANNING) {
                    cameraPermissionState.launchPermissionRequest()
                } else {
                    galleryPermissionState.launchPermissionRequest()
                }
            },
            onDismiss = { currentScreenState.value = ScreenState.EDITING }
        )
        return
    }

    Scaffold(
        topBar = {
            when (currentScreenState.value) {
                ScreenState.SCAN_SELECTION -> CenterAlignedTopAppBar(
                    title = { Text("Scan Document") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (extractedText.value.isNotEmpty()) {
                                currentScreenState.value = ScreenState.EDITING
                            } else {
                                navController.popBackStack()
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                ScreenState.SCANNING -> CenterAlignedTopAppBar(
                    title = { Text("Scanning...") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (extractedText.value.isNotEmpty()) {
                                currentScreenState.value = ScreenState.EDITING
                            } else {
                                currentScreenState.value = ScreenState.SCAN_SELECTION
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )

                ScreenState.EDITING -> TopAppBar(
                    title = { Text("Edit Document") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (extractedText.value.isNotEmpty()) {
                                showDiscardDialog.value = true
                            } else {
                                navController.popBackStack()
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentScreenState.value) {
                ScreenState.SCAN_SELECTION -> {
                    SourceSelectionCard(
                        onCameraSelected = {
                            if (cameraPermissionState.status.isGranted) {
                                currentScreenState.value = ScreenState.SCANNING
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        onGallerySelected = {
                            if (galleryPermissionState.status.isGranted) {
                                galleryLauncher.launch("image/*")
                            } else {
                                galleryPermissionState.launchPermissionRequest()
                            }
                        }
                    )
                }

                ScreenState.SCANNING -> {
                    CameraScanner(
                        onTextDetected = { newText ->
                            extractedText.value = if (extractedText.value.isNotEmpty()) {
                                "${extractedText.value}\n\n$newText"
                            } else {
                                newText
                            }
                            currentScreenState.value = ScreenState.EDITING
                        },
                        onError = { e ->
                            errorMessage.value = e.message ?: "Text recognition failed"
                            showError.value = true
                            currentScreenState.value = ScreenState.SCAN_SELECTION
                        }
                    )
                }

                ScreenState.EDITING -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Text Content
                        OutlinedTextField(
                            value = extractedText.value,
                            onValueChange = { extractedText.value = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            label = { Text("Document Content*") },
                            isError = showError.value && extractedText.value.isEmpty()
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
                                    currentScreenState.value = ScreenState.SCAN_SELECTION
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text("Scan More")
                            }

                            Button(
                                onClick = {
                                    if (titleState.value.isEmpty() ||
                                        descriptionState.value.isEmpty() ||
                                        extractedText.value.isEmpty()
                                    ) {
                                        showError.value = true
                                    } else {
                                        isLoading.value = true
                                        saveToFirestore(
                                            title = titleState.value,
                                            description = descriptionState.value,
                                            extractedText = extractedText.value,
                                            onSuccess = {
                                                isLoading.value = false
                                                showSuccess.value = true
                                            },
                                            onError = {
                                                isLoading.value = false
                                                showError.value = true
                                                errorMessage.value = "Failed to save document"
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading.value
                            ) {
                                if (isLoading.value) {
                                    CircularProgressIndicator(color = Color.White)
                                } else {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showDiscardDialog.value) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog.value = false },
            title = { Text("Discard Changes?") },
            text = { Text("Your scanned text will be lost if you go back. Do you want to continue?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog.value = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    if (showError.value) {
        AlertDialog(
            onDismissRequest = { showError.value = false },
            title = { Text("Error") },
            text = {
                errorMessage.value?.let { Text(it) } ?: Text(
                    when {
                        titleState.value.isEmpty() && descriptionState.value.isEmpty() && extractedText.value.isEmpty() ->
                            "Please enter title, description and document content"
                        titleState.value.isEmpty() -> "Please enter a title"
                        descriptionState.value.isEmpty() -> "Please enter a description"
                        extractedText.value.isEmpty() -> "Document content cannot be empty"
                        else -> "An error occurred"
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
                        navController.popBackStack()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SourceSelectionCard(
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select Source",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = onCameraSelected,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.camera),
                        contentDescription = "Camera",
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Take Photo")
                }
            }

            Button(
                onClick = onGallerySelected,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.gallery),
                        contentDescription = "Gallery",
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Choose from Gallery")
                }
            }
        }
    }
}

@Composable
private fun PermissionDeniedDialog(
    isCamera: Boolean,
    onRequestAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission Required") },
        text = {
            Text(
                if (isCamera) "Camera permission is needed to take photos"
                else "Storage permission is needed to access photos"
            )
        },
        confirmButton = {
            Button(onClick = onRequestAgain) {
                Text("Request Again")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun processUriForText(
    context: Context,
    uri: Uri,
    onTextExtracted: (String) -> Unit,
    onError: () -> Unit
) {
    try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        processImageForText(context, bitmap, onTextExtracted, onError)
    } catch (e: Exception) {
        onError()
        Log.e("ScanAndEditScreen", "Image loading failed", e)
    }
}

private fun processImageForText(
    context: Context,
    bitmap: Bitmap,
    onTextExtracted: (String) -> Unit,
    onError: () -> Unit
) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val inputImage = InputImage.fromBitmap(bitmap, 0)

    recognizer.process(inputImage)
        .addOnSuccessListener { visionText ->
            onTextExtracted(visionText.text)
        }
        .addOnFailureListener { e ->
            onError()
            Log.e("ScanAndEditScreen", "Text recognition failed", e)
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