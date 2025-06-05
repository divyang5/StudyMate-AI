package com.example.studymateai.ui.screen.chapter

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
import com.example.studymateai.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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

private fun captureAndProcessImage(
    imageCapture: ImageCapture?,
    context: Context,
    textRecognizer: TextRecognizer,
    isProcessing: MutableState<Boolean>,
    onTextDetected: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    if (imageCapture == null) {
        onError(Exception("Camera not ready"))
        return
    }

    isProcessing.value = true

    // Create temporary file
    val outputFile = File.createTempFile(
        "IMG_${System.currentTimeMillis()}",
        ".jpg",
        context.externalCacheDir
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                    val image = InputImage.fromBitmap(bitmap, 0)

                    textRecognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            onTextDetected(visionText.text)
                        }
                        .addOnFailureListener { e ->
                            onError(Exception("Text recognition failed", e))
                        }
                        .addOnCompleteListener {
                            isProcessing.value = false
                            outputFile.delete() // Clean up
                        }
                } catch (e: Exception) {
                    isProcessing.value = false
                    outputFile.delete()
                    onError(Exception("Image processing failed", e))
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