package com.divyang.studymateai.ui.screen.scan


import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.Settings
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.divyang.studymateai.ui.components.AppTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.divyang.studymateai.R
import com.divyang.studymateai.navigation.Routes
import com.divyang.studymateai.ui.components.PermissionDialog
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument


enum class ScreenState { SCAN_SELECTION, SCANNING }

/**
 * Source picker + text extraction only. Editing always happens in the shared
 * block editor (TextEditorScreen): after extraction this screen navigates
 * there (or pops back with a result in [returnResult] mode), handing the text
 * over via SavedStateHandle — never through a nav route, where newlines break
 * route matching.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(
    navController: NavController,
    fromCamera: Boolean = false,
    returnResult: Boolean = false,
    context: Context = LocalContext.current
) {
    val scope = rememberCoroutineScope()
    val currentScreenState = remember {
        mutableStateOf(
            if (fromCamera) ScreenState.SCANNING   // "Take Photo" opens the camera directly
            else ScreenState.SCAN_SELECTION
        )
    }
    val isLoading          = remember { mutableStateOf(false) }
    // (current page, page count) while a multi-page PDF is being OCR'd
    val importProgress     = remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val showError          = remember { mutableStateOf(false) }
    val errorMessage       = remember { mutableStateOf<String?>(null) }


    // Permission tracking states
    val hasRequestedCamera = remember { mutableStateOf(false) }
    val hasRequestedGallery = remember { mutableStateOf(false) }
    val showPermissionDialog = remember { mutableStateOf(false) }
    val permissionMessage = remember { mutableStateOf("") }
    val permissionConfirmText = remember { mutableStateOf("Allow") }
    val onPermissionConfirm = remember { mutableStateOf<() -> Unit>({}) }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val galleryPermission = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE
    )

    val onTextReady: (String) -> Unit = { text ->
        if (returnResult) {
            // "Scan More": hand the text back to the editor already on the stack.
            navController.previousBackStackEntry
                ?.savedStateHandle?.set(Routes.KEY_SCANNED_TEXT, text)
            navController.popBackStack()
        } else {
            // Import flow: replace this screen with the editor and hand the
            // text to it the same way.
            navController.navigate(Routes.TextEdit.createRoute()) {
                popUpTo(Routes.Scan.fullRoute) { inclusive = true }
            }
            navController.currentBackStackEntry
                ?.savedStateHandle?.set(Routes.KEY_SCANNED_TEXT, text)
        }
    }

    // Gallery launcher — decode + OCR run off the main thread (a full-size
    // photo decode on Main froze the UI for the whole extraction).
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isLoading.value = true
            scope.launch {
                try {
                    val text = withContext(Dispatchers.Default) {
                        extractTextFromImageUri(context, it)
                    }
                    onTextReady(text)
                } catch (e: Exception) {
                    Log.e("ScanScreen", "Image processing failed", e)
                    errorMessage.value = "Failed to process image"
                    showError.value = true
                } finally {
                    isLoading.value = false
                }
            }
        }
    }

    // PDF / docx / txt launcher
    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isLoading.value = true
            scope.launch {
                try {
                    val text = withContext(Dispatchers.IO) {
                        extractTextFromDocument(context, it) { page, total ->
                            importProgress.value = page to total
                        }
                    }
                    if (text != null) {
                        onTextReady(text)
                    } else {
                        errorMessage.value = "Could not read this file type"
                        showError.value = true
                    }
                } catch (e: Exception) {
                    Log.e("ScanScreen", "Document extraction failed", e)
                    errorMessage.value = "Failed to read this file"
                    showError.value = true
                } finally {
                    isLoading.value = false
                    importProgress.value = null
                }
            }
        }
    }

    BackHandler {
        when (currentScreenState.value) {
            ScreenState.SCAN_SELECTION -> navController.popBackStack()
            ScreenState.SCANNING -> currentScreenState.value = ScreenState.SCAN_SELECTION
        }
    }

    LaunchedEffect(currentScreenState.value) {
        if (currentScreenState.value == ScreenState.SCANNING && !cameraPermission.status.isGranted)
            cameraPermission.launchPermissionRequest()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = when (currentScreenState.value) {
                    ScreenState.SCAN_SELECTION -> "Add Chapter"
                    ScreenState.SCANNING -> "Scan Document"
                },
                onBack = {
                    when (currentScreenState.value) {
                        ScreenState.SCAN_SELECTION -> navController.popBackStack()
                        ScreenState.SCANNING -> currentScreenState.value = ScreenState.SCAN_SELECTION
                    }
                }
            )
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
                                        if (cameraPermission.status.isGranted) {
                                            currentScreenState.value = ScreenState.SCANNING
                                        } else {
                                            if (cameraPermission.status.shouldShowRationale) {
                                                permissionMessage.value = "StudyMate AI needs camera access to scan documents."
                                                permissionConfirmText.value = "Allow"
                                                onPermissionConfirm.value = { cameraPermission.launchPermissionRequest() }
                                                showPermissionDialog.value = true
                                            } else if (hasRequestedCamera.value) {
                                                permissionMessage.value = "Camera permission is permanently denied. Please enable it in App Settings to scan documents."
                                                permissionConfirmText.value = "Open Settings"
                                                onPermissionConfirm.value = {
                                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                        data = Uri.fromParts("package", context.packageName, null)
                                                    }
                                                    context.startActivity(intent)
                                                }
                                                showPermissionDialog.value = true
                                            } else {
                                                hasRequestedCamera.value = true
                                                cameraPermission.launchPermissionRequest()
                                            }
                                        }
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
                                        if (galleryPermission.status.isGranted) {
                                            galleryLauncher.launch("image/*")
                                        } else {
                                            if (galleryPermission.status.shouldShowRationale) {
                                                permissionMessage.value = "StudyMate AI needs gallery access to select document images."
                                                permissionConfirmText.value = "Allow"
                                                onPermissionConfirm.value = { galleryPermission.launchPermissionRequest() }
                                                showPermissionDialog.value = true
                                            } else if (hasRequestedGallery.value) {
                                                permissionMessage.value = "Gallery permission is permanently denied. Please enable it in App Settings to choose images."
                                                permissionConfirmText.value = "Open Settings"
                                                onPermissionConfirm.value = {
                                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                        data = Uri.fromParts("package", context.packageName, null)
                                                    }
                                                    context.startActivity(intent)
                                                }
                                                showPermissionDialog.value = true
                                            } else {
                                                hasRequestedGallery.value = true
                                                galleryPermission.launchPermissionRequest()
                                            }
                                        }
                                    }
                                )
                                ScanDivider()
                                SourceRow(
                                    iconRes  = R.drawable.ic_pdf,
                                    iconBg   = Color(0xFFFAEEDA),
                                    iconTint = Color(0xFF854F0B),
                                    title    = "Import PDF",
                                    subtitle = "Extract text from PDF",
                                    onClick  = { documentLauncher.launch("application/pdf") }
                                )
                                ScanDivider()
                                SourceRow(
                                    iconRes  = R.drawable.ic_document,
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
                        onTextDetected = onTextReady,
                        onError = { e ->
                            errorMessage.value = e.message ?: "Text recognition failed"
                            showError.value = true
                            currentScreenState.value = ScreenState.SCAN_SELECTION
                        }
                    )
                }
            }

            // Global loading overlay
            if (isLoading.value) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF534AB7))
                        importProgress.value?.let { (page, total) ->
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Scanning page $page of $total",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
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

    if (showPermissionDialog.value) {
        PermissionDialog(
            text = permissionMessage.value,
            confirmText = permissionConfirmText.value,
            onDismiss = { showPermissionDialog.value = false },
            onConfirm = {
                showPermissionDialog.value = false
                onPermissionConfirm.value.invoke()
            }
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

// OCR accuracy plateaus well below full sensor resolution; capping the longest
// side keeps per-page bitmaps small enough to avoid GC churn/OOM.
private const val MAX_OCR_DIMENSION = 2048

suspend fun extractTextFromDocument(
    context: Context,
    uri: Uri,
    onProgress: (page: Int, total: Int) -> Unit = { _, _ -> }
): String? {
    val mime = context.contentResolver.getType(uri) ?: ""
    return when {
        mime == "application/pdf" || uri.toString().endsWith(".pdf") ->
            extractTextFromPdf(context, uri, onProgress)

        mime.contains("wordprocessingml") || uri.toString().endsWith(".docx") ->
            extractTextFromDocx(context, uri)

        mime == "text/plain" || uri.toString().endsWith(".txt") ->
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()

        else -> null
    }
}

private suspend fun extractTextFromPdf(
    context: Context,
    uri: Uri,
    onProgress: (page: Int, total: Int) -> Unit
): String {
    val sb = StringBuilder()
    val fd: ParcelFileDescriptor =
        context.contentResolver.openFileDescriptor(uri, "r") ?: return ""
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        PdfRenderer(fd).use { renderer ->
            val pageCount = renderer.pageCount
            for (i in 0 until pageCount) {
                onProgress(i + 1, pageCount)
                renderer.openPage(i).use { page ->
                    // Upscale for OCR quality, but cap the bitmap size —
                    // an unconditional 2x on large pages allocated enormous
                    // ARGB_8888 bitmaps.
                    val scale = (MAX_OCR_DIMENSION.toFloat() / maxOf(page.width, page.height))
                        .coerceAtMost(2f)
                    val bmp = Bitmap.createBitmap(
                        (page.width * scale).toInt().coerceAtLeast(1),
                        (page.height * scale).toInt().coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    // PdfRenderer leaves unpainted pixels transparent, which
                    // OCR reads as black — start from a white page.
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val result = recognizer.process(InputImage.fromBitmap(bmp, 0)).await()
                    sb.append(result.text).append("\n\n")
                    bmp.recycle()
                }
            }
        }
    } finally {
        recognizer.close()
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

/** Decodes the image downsampled to [MAX_OCR_DIMENSION] and OCRs it. Throws on failure. */
private suspend fun extractTextFromImageUri(context: Context, uri: Uri): String {
    val bitmap = decodeDownsampledBitmap(context, uri)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
    } finally {
        recognizer.close()
        bitmap.recycle()
    }
}

private fun decodeDownsampledBitmap(context: Context, uri: Uri): Bitmap =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(
            ImageDecoder.createSource(context.contentResolver, uri)
        ) { decoder, info, _ ->
            // ML Kit needs a software bitmap, and full-resolution photos are
            // far larger than OCR benefits from.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val maxDim = maxOf(info.size.width, info.size.height)
            if (maxDim > MAX_OCR_DIMENSION) {
                decoder.setTargetSampleSize(maxDim / MAX_OCR_DIMENSION)
            }
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = Integer.highestOneBit((maxDim / MAX_OCR_DIMENSION).coerceAtLeast(1))
        }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: throw IllegalStateException("Could not decode image")
    }
