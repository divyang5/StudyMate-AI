package com.example.studymateai.ui.screen.main

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.studymateai.data.model.chapters.Chapter
import com.example.studymateai.navigation.Routes
import com.example.studymateai.shredPrefs.SharedPref
import com.example.studymateai.ui.components.BottomNavigationBar
import com.example.studymateai.ui.components.ChapterCard
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.Firebase
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    context: Context = LocalContext.current
) {
    val quickActions = listOf(
        "Scan Document",
        "Create Quiz",
        "Make Flashcards",
        "Generate Summary"
    )

    val auth = Firebase.auth
    val firestore = Firebase.firestore
    val sharedPref = remember { SharedPref(context) }

    // State for user data
    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(true) }
    val chapters = remember { mutableStateOf<List<Chapter>>(emptyList()) }
    val isChaptersLoading = remember { mutableStateOf(false) }

    // Permission states
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val galleryPermissionState = rememberPermissionState(galleryPermission)

    val showPermissionDialog = remember { mutableStateOf(false) }


    // Fetch user data when screen loads
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted || !galleryPermissionState.status.isGranted) {
            showPermissionDialog.value = true
        }
        val userId = auth.currentUser?.uid ?: return@LaunchedEffect
        try {
            val document = firestore.collection("users").document(userId).get().await()
            val userFirstName = document.getString("firstName") ?: ""
            val userLastName = document.getString("lastName") ?: ""

            // Update state
            firstName.value = userFirstName
            lastName.value = userLastName

            // Save to SharedPreferences
            sharedPref.setPrefString("FIRST_NAME", userFirstName)
            sharedPref.setPrefString("LAST_NAME", userLastName)

            // Update auth display name
            val userProfileChangeRequest = UserProfileChangeRequest.Builder()
                .setDisplayName("$userFirstName $userLastName")
                .build()
            auth.currentUser?.updateProfile(userProfileChangeRequest)

            isLoading.value = false
        } catch (e: Exception) {
            isLoading.value = false
            // Handle error
        }

        isChaptersLoading.value = true
        try {
            val userId = auth.currentUser?.uid ?: return@LaunchedEffect
            val querySnapshot = firestore.collection("chapters")
                .whereEqualTo("userId", userId)
//                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()

            val chaptersList = querySnapshot.documents.map { doc ->
                Chapter(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    content = doc.getString("content") ?: "",
                    createdAt = doc.getDate("createdAt") ?: Date()
                )
            }
            chapters.value = chaptersList
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error fetching chapters", e)
        } finally {
            isChaptersLoading.value = false
        }
    }

    if (showPermissionDialog.value) {
        PermissionDialog(
            onDismiss = { showPermissionDialog.value = false },
            onConfirm = {
                cameraPermissionState.launchPermissionRequest()
                galleryPermissionState.launchPermissionRequest()
            }
        )
    }

    if (cameraPermissionState.status.isGranted && galleryPermissionState.status.isGranted) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Scan Document Screen")
        }
    }
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        if (isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Personalized header
                Text(
                    text = "Welcome Back, ${firstName.value}!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Rest of your existing UI...
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(quickActions) { action ->
                        QuickActionCard(action) {
                            when (action) {
                                "Scan Document" -> navController.navigate(Routes.Scan.route)
                                "Create Quiz" -> navController.navigate(Routes.QuizGen.route)
                                // Add other actions
                            }
                        }
                    }
                }

                Text(
                    text = "Recent Chapters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (isChaptersLoading.value) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    if (chapters.value.isEmpty()) {
                        Text(
                            text = "No chapters found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(chapters.value) { chapter ->
                                ChapterCard(
                                    chapter = chapter,
                                    onClick = {
//                                        navController.navigate(" ")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(text: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier
            .width(150.dp)
            .height(120.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions Needed") },
        text = { Text("StudyMate AI needs camera and gallery permissions to scan documents") },
        confirmButton = {
            Button(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}