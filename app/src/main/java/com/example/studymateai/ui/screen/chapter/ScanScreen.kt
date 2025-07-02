package com.example.studymateai.ui.screen.chapter

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.example.studymateai.R
import com.example.studymateai.navigation.Routes
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    fromCamera: Boolean,
    existingText: String? = null,
    navController: NavController,
    context: Context = LocalContext.current
) {
    // Permission states
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val galleryPermissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    // UI state
    val showSourceSelector = remember { mutableStateOf(!fromCamera) }
    val selectedImageUri = remember { mutableStateOf<Uri?>(null) }
    val extractedText = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val showError = remember { mutableStateOf(false) }
    var errorMessage = remember { mutableStateOf<String?>(null) }

    val showCameraScanner = remember { mutableStateOf(fromCamera) }

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
                    // Combine with existing text if available
                    val combinedText = if (!existingText.isNullOrEmpty()) {
                        "$existingText\n\n$newText"
                    } else {
                        newText
                    }
                    val encodedText = URLEncoder.encode(combinedText, "UTF-8")
                    navController.navigate(Routes.TextEdit.createRoute(encodedText)) {
                        popUpTo(Routes.Scan.route) { inclusive = true }
                    }
                },
                onError = {
                    isLoading.value = false
                    showError.value = true
                }
            )
        }
    }

    // Check permissions
    LaunchedEffect(Unit) {
        if (fromCamera && !cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        } else if (!fromCamera && !galleryPermissionState.status.isGranted) {
            galleryPermissionState.launchPermissionRequest()
        }
    }

    // Permission denied handling
    if ((fromCamera && !cameraPermissionState.status.isGranted) ||
        (!fromCamera && !galleryPermissionState.status.isGranted)) {
        PermissionDeniedDialog(
            isCamera = fromCamera,
            onRequestAgain = {
                if (fromCamera) cameraPermissionState.launchPermissionRequest()
                else galleryPermissionState.launchPermissionRequest()
            },
            onDismiss = { navController.popBackStack() }
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Scan Document") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (showSourceSelector.value) {
                SourceSelectionCard(
                    onCameraSelected = {
                        showSourceSelector.value = false
                        if (cameraPermissionState.status.isGranted) {
                            showSourceSelector.value = false
                            showCameraScanner.value = true
                        }
                    },
                    onGallerySelected = {
                        showSourceSelector.value = false
                        if (galleryPermissionState.status.isGranted) {
                            showSourceSelector.value = false
                            galleryLauncher.launch("image/*")
                        }
                    }
                )
            } else {
                when {
                    showSourceSelector.value -> {
                        SourceSelectionCard(
                            onCameraSelected = {
                                showSourceSelector.value = false
                                showCameraScanner.value = true
                            },
                            onGallerySelected = {
                                showSourceSelector.value = false
                                galleryLauncher.launch("image/*")
                            }
                        )
                    }

                    showCameraScanner.value -> {
                        Box(modifier = Modifier.weight(1f)) {
                            CameraScanner(
                                onTextDetected = { newText ->
                                    // Combine with existing text if available
                                    val combinedText = if (!existingText.isNullOrEmpty()) {
                                        "$existingText\n\n$newText"
                                    } else {
                                        newText
                                    }
                                    val encodedText = URLEncoder.encode(combinedText, "UTF-8")
                                    navController.navigate(Routes.TextEdit.createRoute(encodedText)) {
                                        popUpTo(Routes.Scan.route) { inclusive = true }
                                    }
                                },
                                onError = { e ->
                                    errorMessage.value = e.message
                                    Log.e("ScanScreen", "Text recognition failed" + e.message, e)
                                }
                            )

                            // Close button for camera view
//                            IconButton(
//                                onClick = { showCameraScanner.value = false },
//                                modifier = Modifier
//                                    .align(Alignment.TopStart)
//                                    .padding(16.dp)
//                            ) {
//                                Icon(Icons.Default.Close, contentDescription = "Close camera")
//                            }

                            errorMessage.value?.let { error ->
                                Text("Error: $error", color = Color.Red)
                                Log.e("ScanScreen", "Text recognition failed" + error)
                            }
                        }
                    }
                    isLoading.value -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    if (showError.value) {
        AlertDialog(
            onDismissRequest = { showError.value = false },
            title = { Text("Error") },
            text = { Text("Failed to process image. Please try again.") },
            confirmButton = {
                Button(onClick = { showError.value = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun SourceSelectionCard(
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
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
fun ScannedTextContent(
    text: String,
    onRescan: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Extracted Text",
            style = MaterialTheme.typography.titleLarge
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            )
        }

        Button(
            onClick = onRescan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Again")
        }
    }
}

@Composable
fun ImagePreview(
    uri: Uri,
    onRescan: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Selected Image",
            style = MaterialTheme.typography.titleLarge
        )

        Image(
            painter = rememberImagePainter(uri),
            contentDescription = "Selected image",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )

        Button(
            onClick = onRescan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Rescan")
        }
    }
}

@Composable
fun PermissionDeniedDialog(
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

// Updated text processing functions
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
        Log.e("ScanScreen", "Image loading failed", e)
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
            Log.e("ScanScreen", "Text recognition failed", e)
        }
}