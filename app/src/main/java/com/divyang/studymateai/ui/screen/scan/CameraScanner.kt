package com.divyang.studymateai.ui.screen.scan

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.divyang.studymateai.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File


@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraScanner(
    onTextDetected: (String) -> Unit,
    onError: (Exception) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val cameraProviderState = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }
    val isProcessing = remember { mutableStateOf(false) }

    // Initialize camera provider
    LaunchedEffect(Unit) {
        try {
            val provider = ProcessCameraProvider.getInstance(context).await()
            cameraProviderState.value = provider
        } catch (e: Exception) {
            onError(Exception("Camera initialization failed", e))
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                cameraProviderState.value?.let { cameraProvider ->
                    // Set up the preview use case
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(view.surfaceProvider)
                    }

                    // Set up image capture use case
                    val imageCaptureInstance = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(view.display.rotation)
                        .build()
                    imageCapture.value = imageCaptureInstance

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCaptureInstance
                        )
                    } catch (e: Exception) {
                        onError(Exception("Camera binding failed", e))
                    }
                }
            }
        )

        // Capture button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Button(
                onClick = {
                    if (!isProcessing.value) {
                        captureAndProcessImage(
                            imageCapture = imageCapture.value,
                            context = context,
                            textRecognizer = textRecognizer,
                            scope = scope,
                            isProcessing = isProcessing,
                            onTextDetected = onTextDetected,
                            onError = onError
                        )
                    }
                },
                enabled = !isProcessing.value
            ) {
                if (isProcessing.value) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.camera),
                        contentDescription = "Capture",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

// OCR accuracy plateaus well below full sensor resolution; capping the longest
// side keeps the decoded bitmap small enough to avoid GC churn/OOM.
private const val MAX_OCR_DIMENSION = 2048

private fun captureAndProcessImage(
    imageCapture: ImageCapture?,
    context: Context,
    textRecognizer: TextRecognizer,
    scope: CoroutineScope,
    isProcessing: MutableState<Boolean>,
    onTextDetected: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    if (imageCapture == null) {
        onError(Exception("Camera not ready"))
        return
    }

    isProcessing.value = true

    // Create temporary file in app-internal cache (not world-accessible external
    // storage). The captured image is deleted right after OCR completes.
    val outputFile = File.createTempFile(
        "IMG_${System.currentTimeMillis()}",
        ".jpg",
        context.cacheDir
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // Decode + OCR off the main thread — a full-resolution camera
                // JPEG decode on Main froze the preview.
                scope.launch(Dispatchers.Default) {
                    try {
                        val bitmap = decodeDownsampledBitmap(outputFile.absolutePath)
                        val text = textRecognizer
                            .process(InputImage.fromBitmap(bitmap, 0)).await().text
                        bitmap.recycle()
                        withContext(Dispatchers.Main) { onTextDetected(text) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            onError(Exception("Image processing failed", e))
                        }
                    } finally {
                        outputFile.delete()
                        withContext(Dispatchers.Main) { isProcessing.value = false }
                    }
                }
            }

            override fun onError(ex: ImageCaptureException) {
                isProcessing.value = false
                outputFile.delete()
                onError(Exception("Image capture failed", ex))
            }
        }
    )
}

private fun decodeDownsampledBitmap(path: String): android.graphics.Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
    val opts = BitmapFactory.Options().apply {
        inSampleSize = Integer.highestOneBit((maxDim / MAX_OCR_DIMENSION).coerceAtLeast(1))
    }
    return BitmapFactory.decodeFile(path, opts)
        ?: throw IllegalStateException("Could not decode captured image")
}