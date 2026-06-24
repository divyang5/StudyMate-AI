package com.example.studymateai.ui.screen.chapter


import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument


enum class ScreenState { SCAN_SELECTION, SCANNING, EDITING }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    navController: NavController,
    initialText: String? = null,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val currentScreenState = remember {
        mutableStateOf(
            if (initialText.isNullOrEmpty()) ScreenState.SCAN_SELECTION else ScreenState.EDITING
        )
    }
    val showDiscardDialog  = remember { mutableStateOf(false) }
    val extractedText      = remember { mutableStateOf(initialText ?: "") }
    val titleState         = remember { mutableStateOf("") }
    val descriptionState   = remember { mutableStateOf("") }
    val isLoading          = remember { mutableStateOf(false) }
    val showError          = remember { mutableStateOf(false) }
    val errorMessage       = remember { mutableStateOf<String?>(null) }
    val showSuccess        = remember { mutableStateOf(false) }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val galleryPermission = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE
    )

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isLoading.value = true
            processUriForText(context, it,
                onTextExtracted = { text ->
                    extractedText.value = appendText(extractedText.value, text)
                    isLoading.value = false
                    currentScreenState.value = ScreenState.EDITING
                },
                onError = {
                    isLoading.value = false
                    errorMessage.value = "Failed to process image"
                    showError.value = true
                }
            )
        }
    }

    // PDF / docx / txt launcher
    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isLoading.value = true
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    extractTextFromDocument(context, it)
                }
                if (text != null) {
                    extractedText.value = appendText(extractedText.value, text)
                    currentScreenState.value = ScreenState.EDITING
                } else {
                    errorMessage.value = "Could not read this file type"
                    showError.value = true
                }
                isLoading.value = false
            }
        }
    }

    BackHandler {
        when (currentScreenState.value) {
            ScreenState.SCAN_SELECTION -> navController.popBackStack()
            ScreenState.SCANNING -> currentScreenState.value =
                if (extractedText.value.isNotEmpty()) ScreenState.EDITING else ScreenState.SCAN_SELECTION
            ScreenState.EDITING -> if (extractedText.value.isNotEmpty()) showDiscardDialog.value = true
            else navController.popBackStack()
        }
    }

    LaunchedEffect(currentScreenState.value) {
        if (currentScreenState.value == ScreenState.SCANNING && !cameraPermission.status.isGranted)
            cameraPermission.launchPermissionRequest()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentScreenState.value) {
                                ScreenState.SCAN_SELECTION -> "Add Chapter"
                                ScreenState.SCANNING       -> "Scan Document"
                                ScreenState.EDITING        -> "Edit Document"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            when (currentScreenState.value) {
                                ScreenState.SCAN_SELECTION -> navController.popBackStack()
                                ScreenState.SCANNING -> currentScreenState.value =
                                    if (extractedText.value.isNotEmpty()) ScreenState.EDITING
                                    else ScreenState.SCAN_SELECTION
                                ScreenState.EDITING -> if (extractedText.value.isNotEmpty())
                                    showDiscardDialog.value = true
                                else navController.popBackStack()
                            }
                        }) {
                            Surface(shape = CircleShape, color = Color(0xFFEEEDFE)) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF534AB7),
                                    modifier = Modifier.size(32.dp).padding(6.dp)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentScreenState.value) {

                // ── Source Selection ──────────────────────────────────────
                ScreenState.SCAN_SELECTION -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CHOOSE SOURCE",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        Card(
                            shape  = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                SourceRow(
                                    iconRes     = R.drawable.camera,
                                    iconBg      = Color(0xFFEEEDFE),
                                    iconTint    = Color(0xFF534AB7),
                                    title       = "Take Photo",
                                    subtitle    = "Scan with camera",
                                    onClick     = {
                                        if (cameraPermission.status.isGranted)
                                            currentScreenState.value = ScreenState.SCANNING
                                        else cameraPermission.launchPermissionRequest()
                                    }
                                )
                                ScanDivider()
                                SourceRow(
                                    iconRes  = R.drawable.gallery,
                                    iconBg   = Color(0xFFE1F5EE),
                                    iconTint = Color(0xFF0F6E56),
                                    title    = "Choose from Gallery",
                                    subtitle = "Pick an image file",
                                    onClick  = {
                                        if (galleryPermission.status.isGranted)
                                            galleryLauncher.launch("image/*")
                                        else galleryPermission.launchPermissionRequest()
                                    }
                                )
                                ScanDivider()
                                SourceRow(
                                    iconRes  = R.drawable.ic_pdf,       // add a pdf drawable
                                    iconBg   = Color(0xFFFAEEDA),
                                    iconTint = Color(0xFF854F0B),
                                    title    = "Import PDF",
                                    subtitle = "Extract text from PDF",
                                    onClick  = { documentLauncher.launch("application/pdf") }
                                )
                                ScanDivider()
                                SourceRow(
                                    iconRes  = R.drawable.ic_document,  // add a doc drawable
                                    iconBg   = Color(0xFFFAECE7),
                                    iconTint = Color(0xFF993C1D),
                                    title    = "Import Document",
                                    subtitle = ".txt · .docx files",
                                    onClick  = {
                                        documentLauncher.launch("*/*")
                                        // mime filter handled in extractor
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Camera ────────────────────────────────────────────────
                ScreenState.SCANNING -> {
                    CameraScanner(
                        onTextDetected = { newText ->
                            extractedText.value = appendText(extractedText.value, newText)
                            currentScreenState.value = ScreenState.EDITING
                        },
                        onError = { e ->
                            errorMessage.value = e.message ?: "Text recognition failed"
                            showError.value = true
                            currentScreenState.value = ScreenState.SCAN_SELECTION
                        }
                    )
                }

                // ── Editing ───────────────────────────────────────────────
                ScreenState.EDITING -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))

                        ScanSectionLabel("Content")
                        OutlinedTextField(
                            value = extractedText.value,
                            onValueChange = { extractedText.value = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 320.dp),
                            placeholder = { Text("Document text...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) },
                            isError = showError.value && extractedText.value.isEmpty(),
                            shape = RoundedCornerShape(14.dp),
                            maxLines = 20,
                            colors = scanFieldColors()
                        )

                        Spacer(Modifier.height(4.dp))
                        ScanSectionLabel("Title")
                        OutlinedTextField(
                            value = titleState.value,
                            onValueChange = { titleState.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Chapter title", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) },
                            isError = showError.value && titleState.value.isEmpty(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = scanFieldColors()
                        )

                        Spacer(Modifier.height(4.dp))
                        ScanSectionLabel("Description")
                        OutlinedTextField(
                            value = descriptionState.value,
                            onValueChange = { descriptionState.value = it },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text("Short description", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) },
                            isError = showError.value && descriptionState.value.isEmpty(),
                            shape = RoundedCornerShape(14.dp),
                            maxLines = 5,
                            colors = scanFieldColors()
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentScreenState.value = ScreenState.SCAN_SELECTION },
                                shape  = RoundedCornerShape(14.dp),
                                border = BorderStroke(0.5.dp, Color(0xFF534AB7)),
                                modifier = Modifier.weight(1f).height(52.dp)
                            ) {
                                Text("Scan More", color = Color(0xFF534AB7))
                            }
                            Button(
                                onClick = {
                                    if (titleState.value.isEmpty() || descriptionState.value.isEmpty() || extractedText.value.isEmpty()) {
                                        showError.value = true
                                    } else {
                                        isLoading.value = true
                                        saveToFirestore(
                                            title         = titleState.value,
                                            description   = descriptionState.value,
                                            extractedText = extractedText.value,
                                            onSuccess     = { isLoading.value = false; showSuccess.value = true },
                                            onError       = { isLoading.value = false; errorMessage.value = "Failed to save"; showError.value = true }
                                        )
                                    }
                                },
                                shape  = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF534AB7),
                                    contentColor   = Color.White
                                ),
                                modifier  = Modifier.weight(1f).height(52.dp),
                                enabled   = !isLoading.value,
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                if (isLoading.value) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Text("Save", fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            // Global loading overlay
            if (isLoading.value) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF534AB7))
                }
            }
        }
    }

    // Discard dialog
    if (showDiscardDialog.value) {
        StyledDialog(
            title = "Discard Changes?",
            body  = "Your scanned text will be lost if you go back.",
            confirmText  = "Discard",
            confirmColor = Color(0xFFA32D2D),
            confirmBg    = Color(0xFFFCEBEB),
            onConfirm    = { showDiscardDialog.value = false; navController.popBackStack() },
            onDismiss    = { showDiscardDialog.value = false }
        )
    }

    // Error dialog
    if (showError.value && errorMessage.value != null) {
        StyledDialog(
            title = "Error",
            body  = errorMessage.value ?: "Something went wrong",
            confirmText  = "OK",
            confirmColor = Color(0xFF534AB7),
            confirmBg    = Color(0xFFEEEDFE),
            onConfirm    = { showError.value = false; errorMessage.value = null },
            onDismiss    = { showError.value = false; errorMessage.value = null }
        )
    }

    // Success dialog
    if (showSuccess.value) {
        StyledDialog(
            title = "Saved!",
            body  = "Chapter saved successfully.",
            confirmText  = "Continue",
            confirmColor = Color(0xFF0F6E56),
            confirmBg    = Color(0xFFE1F5EE),
            onConfirm    = { showSuccess.value = false; navController.popBackStack() },
            onDismiss    = { showSuccess.value = false }
        )
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun SourceRow(
    iconRes: Int,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .background(iconBg, RoundedCornerShape(11.dp))
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Icon(
            painter = painterResource(R.drawable.ic_forward),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ScanDivider() = HorizontalDivider(
    modifier = Modifier.padding(horizontal = 16.dp),
    thickness = 0.5.dp,
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
)

@Composable
private fun ScanSectionLabel(text: String) = Text(
    text = text.uppercase(),
    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
)

@Composable
private fun scanFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Color(0xFF534AB7),
    unfocusedBorderColor = Color(0xFF534AB7).copy(alpha = 0.25f),
    focusedLabelColor    = Color(0xFF534AB7),
    cursorColor          = Color(0xFF534AB7)
)

@Composable
private fun StyledDialog(
    title: String,
    body: String,
    confirmText: String,
    confirmColor: Color,
    confirmBg: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text  = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = confirmBg, contentColor = confirmColor),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) { Text(confirmText, fontWeight = FontWeight.Medium) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// ── Document text extraction ──────────────────────────────────────────────────

fun extractTextFromDocument(context: Context, uri: Uri): String? {
    val mime = context.contentResolver.getType(uri) ?: ""
    return when {
        mime == "application/pdf" || uri.toString().endsWith(".pdf") ->
            extractTextFromPdf(context, uri)

        mime.contains("wordprocessingml") || uri.toString().endsWith(".docx") ->
            extractTextFromDocx(context, uri)

        mime == "text/plain" || uri.toString().endsWith(".txt") ->
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()

        else -> null
    }
}

private fun extractTextFromPdf(context: Context, uri: Uri): String {
    val sb = StringBuilder()
    val fd: ParcelFileDescriptor =
        context.contentResolver.openFileDescriptor(uri, "r") ?: return ""
    PdfRenderer(fd).use { renderer ->
        for (i in 0 until renderer.pageCount) {
            renderer.openPage(i).use { page ->
                // Render each page to bitmap then OCR it
                val bmp = Bitmap.createBitmap(
                    page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888
                )
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val image = InputImage.fromBitmap(bmp, 0)
                val result = com.google.android.gms.tasks.Tasks.await(
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                )
                sb.append(result.text).append("\n\n")
                bmp.recycle()
            }
        }
    }
    return sb.toString().trim()
}

private fun extractTextFromDocx(context: Context, uri: Uri): String {
    val sb = StringBuilder()
    context.contentResolver.openInputStream(uri)?.use { stream ->
        XWPFDocument(stream).paragraphs.forEach { para ->
            sb.appendLine(para.text)
        }
    }
    return sb.toString().trim()
}

private fun appendText(existing: String, new: String): String =
    if (existing.isNotEmpty()) "$existing\n\n$new" else new

private fun saveToFirestore(
    title: String, description: String, extractedText: String,
    onSuccess: () -> Unit, onError: () -> Unit
) {
    val userId = Firebase.auth.currentUser?.uid ?: return onError()
    Firebase.firestore.collection("chapters").add(
        hashMapOf(
            "title"       to title,
            "description" to description,
            "content"     to extractedText,
            "createdAt"   to FieldValue.serverTimestamp(),
            "userId"      to userId
        )
    ).addOnSuccessListener { onSuccess() }.addOnFailureListener { onError() }
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
