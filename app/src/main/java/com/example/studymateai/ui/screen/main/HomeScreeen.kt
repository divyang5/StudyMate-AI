package com.example.studymateai.ui.screen.main

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.studymateai.R
import com.example.studymateai.ads.AdManager
import com.example.studymateai.data.viewmodel.HomeUiState
import com.example.studymateai.data.viewmodel.HomeViewModel
import com.example.studymateai.navigation.Routes
import com.example.studymateai.ui.components.BottomNavigationBar
import com.example.studymateai.ui.components.ChapterCard
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

// ─── Screen ─────────────────────────────────────────────────────────────────

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class,
    ExperimentalMaterialApi::class
)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val adManager = remember { AdManager(context) }

    // ── Permissions ──────────────────────────────────────────────────────────
    val galleryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

    val cameraPermission  = rememberPermissionState(Manifest.permission.CAMERA)
    val galleryPermission2 = rememberPermissionState(galleryPermission)

    var showPermissionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted || !galleryPermission2.status.isGranted) {
            showPermissionDialog = true
        }
    }

    // ── Error Snackbar ───────────────────────────────────────────────────────
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // ── Pull-to-Refresh ──────────────────────────────────────────────────────
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh  = viewModel::refresh
    )

    // ── Permission Dialog ────────────────────────────────────────────────────
    if (showPermissionDialog) {
        PermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onConfirm = {
                cameraPermission.launchPermissionRequest()
                galleryPermission2.launchPermissionRequest()
                showPermissionDialog = false
            }
        )
    }

    Scaffold(
        modifier      = Modifier.background(MaterialTheme.colorScheme.background),
        bottomBar     = { BottomNavigationBar(navController) },
        snackbarHost  = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (uiState.isUserLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top    = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 10.dp
                )
        ) {
            // ── Sticky App Bar ───────────────────────────────────────────────
//            WelcomeHeader()

            TopAppBar(
                title = {
                    Text(
                        text = "Studymate AI",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
            // ── Banner Ad ────────────────────────────────────────────────────
            adManager.BannerAd(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            // ── Scrollable Content + Pull Refresh ────────────────────────────
            Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
                HomeContent(
                    uiState       = uiState,
                    navController = navController
                )

                PullRefreshIndicator(
                    refreshing = uiState.isRefreshing,
                    state      = pullRefreshState,
                    modifier   = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

// ─── Home Content ────────────────────────────────────────────────────────────

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    navController: NavController
) {
    LazyColumn(
        modifier            = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        state               = rememberLazyListState()
    ) {
        // Quick Actions header + row
        item {
            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SectionTitle("Quick Actions")
                QuickActionsRow(navController)
                SectionTitle("Recent Chapters")
            }
        }

        // Chapters list
        when {
            uiState.isChaptersLoading -> item {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }

            uiState.chapters.isEmpty() -> item {
                Text(
                    text     = "No chapters yet. Scan a document to get started!",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            else -> items(uiState.chapters, key = { it.id }) { chapter ->
                ChapterCard(
                    chapter  = chapter,
                    onClick  = { navController.navigate(Routes.ChapterDetail.createRoute(chapter.id)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ─── Quick Actions ───────────────────────────────────────────────────────────

private data class QuickAction(val label: String, val route: () -> String)

private val quickActions = listOf(
    "Scan Document"
    // "Create Quiz", "Make Flashcards", "Generate Summary"  ← uncomment when ready
)

@Composable
private fun QuickActionsRow(navController: NavController) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier              = Modifier.fillMaxWidth()
    ) {
        items(quickActions) { action ->
            QuickActionCard(text = action) {
                when (action) {
                    "Scan Document" -> navController.navigate(Routes.Scan.createRoute(fromCamera = false))
                    "Create Quiz"   -> navController.navigate(Routes.QuizGen.route)
                }
            }
        }
    }
}

// ─── Reusable Components ─────────────────────────────────────────────────────

@Composable
private fun SectionTitle(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(text: String, onClick: () -> Unit) {
    Card(
        onClick  = onClick,
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor   = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
            .width(150.dp)
            .height(120.dp)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color  = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape  = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector     = Icons.Default.Add,   // swap per action if needed
                    contentDescription = text,
                    tint            = MaterialTheme.colorScheme.primary,
                    modifier        = Modifier.size(24.dp)
                )
            }

            Text(
                text       = text,
                style      = MaterialTheme.typography.labelLarge,
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
        title            = { Text("Permissions Needed") },
        text             = { Text("StudyMate AI needs camera and gallery access to scan documents.") },
        confirmButton    = {
            Button(onClick = onConfirm) { Text("Allow") }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("Not Now") }
        }
    )
}

@Composable
fun WelcomeHeader() {
    val fontProvider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage   = "com.google.android.gms",
        certificates      = R.array.com_google_android_gms_fonts_certs
    )
    val mrDafoeFontFamily = FontFamily(
        Font(
            googleFont    = GoogleFont("Mr Dafoe"),
            fontProvider  = fontProvider,
            weight        = FontWeight.Bold
        )
    )

    Box(
        modifier          = Modifier
            .fillMaxWidth().padding(start = 10.dp)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment  = Alignment.CenterStart
    ) {
        Text(
            text       = "Studymate AI",
//            style      = MaterialTheme.typography.headlineMedium,
            style      = MaterialTheme.typography.titleLarge,
            color      = MaterialTheme.colorScheme.primary,
//            fontWeight = FontWeight.Bold,
//            fontFamily = mrDafoeFontFamily,
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        )
    }
}

