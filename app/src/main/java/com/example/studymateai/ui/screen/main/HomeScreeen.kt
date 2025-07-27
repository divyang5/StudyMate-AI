package com.example.studymateai.ui.screen.main

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.studymateai.R
import com.example.studymateai.ads.AdManager
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class,
    ExperimentalMaterialApi::class
)
@Composable
fun HomeScreen(
    navController: NavController,
    context: Context = LocalContext.current
) {
    val quickActions = listOf(
        "Scan Document",
//        "Create Quiz",
//        "Make Flashcards",
//        "Generate Summary"
    )

    val auth = Firebase.auth
    val firestore = Firebase.firestore
    val sharedPref = remember { SharedPref(context) }
    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val adManager = remember { AdManager(context) }

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
            Log.d("HomeScreen", "User" + userFirstName + userLastName  )

            isLoading.value = false
        } catch (e: Exception) {
            isLoading.value = false
            Log.e("HomeScreen", "Error fetching User", e)
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

            val chaptersList = querySnapshot.documents
                .mapNotNull { doc ->
                    doc.getDate("createdAt")?.let { date ->
                        Chapter(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            content = doc.getString("content") ?: "",
                            createdAt = date
                        )
                    }
                }
                .sortedByDescending { it.createdAt }
            chapters.value = chaptersList
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error fetching chapters", e)
        } finally {
            isChaptersLoading.value = false
        }
    }

    // Create a function to handle refresh
    fun refreshData() {
        refreshScope.launch {
            refreshing = true
            // Re-fetch all data
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                // Refresh user data
                val document = firestore.collection("users").document(userId).get().await()
                firstName.value = document.getString("firstName") ?: ""
                lastName.value = document.getString("lastName") ?: ""

                // Refresh chapters
                isChaptersLoading.value = true
                val querySnapshot = firestore.collection("chapters")
                    .whereEqualTo("userId", userId)
                    .limit(5)
                    .get()
                    .await()

                chapters.value = querySnapshot.documents
                    .mapNotNull { doc ->
                        doc.getDate("createdAt")?.let { date ->
                            Chapter(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                content = doc.getString("content") ?: "",
                                createdAt = date
                            )
                        }
                    }
                    .sortedByDescending { it.createdAt }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Refresh failed", e)
            } finally {
                refreshing = false
                isChaptersLoading.value = false
            }
        }
    }
    val refreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { refreshData() }
    )

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
                    .padding(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding()+ 10.dp)
            ) {
                // Sticky Header (non-scrollable)
                WelcomeHeader()

                adManager.BannerAd(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )


                Box(modifier = Modifier.pullRefresh(refreshState)) {
                    LazyColumn(
                        modifier = Modifier
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        state = rememberLazyListState()
                    ) {
                        item {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // Quick Actions Section
                                Text(
                                    text = "Quick Actions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(quickActions) { action ->
                                        QuickActionCard(action) {
                                            when (action) {
                                                "Scan Document" -> navController.navigate(
                                                    Routes.Scan.createRoute(
                                                        fromCamera = false
                                                    )
                                                )

                                                "Create Quiz" -> navController.navigate(Routes.QuizGen.route)
                                            }
                                        }
                                    }
                                }

                                // Recent Chapters Section
                                Text(
                                    text = "Recent Chapters",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Chapters List
                        if (isChaptersLoading.value) {
                            item {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        } else if (chapters.value.isEmpty()) {
                            item {
                                Text(
                                    text = "No chapters found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else {
                            items(chapters.value) { chapter ->
                                ChapterCard(
                                    chapter = chapter,
                                    onClick = {
                                        navController.navigate(
                                            Routes.ChapterDetail.createRoute(
                                                chapter.id
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }

                    PullRefreshIndicator(
                        refreshing = refreshing,
                        state = refreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
            .width(150.dp)
            .height(120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (text) {
                        "Scan Document" -> Icons.Default.Add
//                        "Create Quiz" -> Icons.Default.Quiz
                        else -> Icons.Default.Add
                    },
                    contentDescription = text,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
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

@Composable
fun WelcomeHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "StudyMate AI Logo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "StudyMate AI",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )

            }


        }
    }
}

