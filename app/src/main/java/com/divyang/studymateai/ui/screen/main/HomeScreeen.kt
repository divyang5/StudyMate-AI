package com.divyang.studymateai.ui.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.divyang.studymateai.R
import com.divyang.studymateai.ads.AdManager
import com.divyang.studymateai.ads.rememberAdManager
import com.divyang.studymateai.gemini.GeminiAccessState
import com.divyang.studymateai.gemini.GenerationQuota
import com.divyang.studymateai.gemini.rememberGeminiAccessState
import com.divyang.studymateai.data.viewmodel.HomeUiState
import com.divyang.studymateai.data.viewmodel.HomeViewModel
import com.divyang.studymateai.navigation.Routes
import com.divyang.studymateai.ui.components.AppColors
import com.divyang.studymateai.ui.components.BottomNavigationBar
import com.divyang.studymateai.ui.components.ChapterCard
import com.divyang.studymateai.ui.components.GlassActionTile
import com.divyang.studymateai.ui.components.GradientHero
import com.divyang.studymateai.ui.components.HeroStatPill
import com.divyang.studymateai.ui.components.SectionHeading
import com.divyang.studymateai.ui.components.verticalScrollbar
import java.util.Calendar

// ─── Screen ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    savedStateHandle: SavedStateHandle? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val adManager = rememberAdManager()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Import/edit flows set this flag instead of re-navigating to Home, so
    // only the chapter list refetches (not the whole screen + user profile).
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow(Routes.KEY_CHAPTERS_CHANGED, false)?.collect { changed ->
            if (changed) {
                savedStateHandle[Routes.KEY_CHAPTERS_CHANGED] = false
                viewModel.refreshChapters()
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = viewModel::refresh
    )

    // Once per app open: offer the personal-key form to free-plan users.
    val geminiAccess = rememberGeminiAccessState()
    var showKeyPrompt by remember {
        mutableStateOf(!apiKeyPromptShownThisSession && !geminiAccess.hasPersonalKey)
    }
    if (showKeyPrompt) {
        apiKeyPromptShownThisSession = true
        ApiKeyPromptDialog(
            remainingToday = geminiAccess.remainingToday,
            onAddKey = {
                showKeyPrompt = false
                navController.navigate(Routes.GeminiKeySettings.route)
            },
            onDismiss = { showKeyPrompt = false }
        )
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isUserLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            HomeContent(
                uiState = uiState,
                geminiAccess = geminiAccess,
                navController = navController,
                adManager = adManager
            )

            PullRefreshIndicator(
                refreshing = uiState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

// ─── Home Content ────────────────────────────────────────────────────────────

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    geminiAccess: GeminiAccessState,
    navController: NavController,
    adManager: AdManager
) {
    val greeting = remember(uiState.displayName) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val part = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        "$part, ${uiState.displayName} 👋"
    }

    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.verticalScrollbar(listState),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Hero ───────────────────────────────────────────────────────────
        item {
            GradientHero(
                title = greeting,
                subtitle = "Ready to learn something new?",
                trailing = if (geminiAccess.hasPersonalKey) {
                    { KeyActiveBadge() }
                } else null,
                stats = {
                    HeroStatPill(value = "${uiState.chapters.size}", label = "Recent chapters")
                    if (geminiAccess.hasPersonalKey) {
                        HeroStatPill(value = "∞", label = "AI generations")
                    } else {
                        HeroStatPill(
                            value = "${geminiAccess.remainingToday}",
                            label = "Free AI left today"
                        )
                    }
                }
            )
        }

        // ── Add a chapter ──────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeading("Add a chapter")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassActionTile(
                        iconRes = R.drawable.gallery,
                        label = "Scan",
                        subtitle = "Photos/PDF",
                        onClick = { navController.navigate(Routes.Scan.createRoute(fromCamera = false)) },
                        modifier = Modifier.weight(1f)
                    )
                    GlassActionTile(
                        iconRes = R.drawable.camera,
                        label = "Photo",
                        subtitle = "Take a photo",
                        onClick = { navController.navigate(Routes.Scan.createRoute(fromCamera = true)) },
                        modifier = Modifier.weight(1f)
                    )
                    GlassActionTile(
                        iconRes = R.drawable.ic_document,
                        label = "Write",
                        subtitle = "Type it out",
                        onClick = { navController.navigate(Routes.TextEdit.createRoute()) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Banner ad (below the fold) ─────────────────────────────────────
        item {
            adManager.BannerAd(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        // ── Recent chapters ────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeading("Recent chapters")
                if (uiState.chapters.isNotEmpty()) {
                    TextButton(onClick = { navController.navigate(Routes.Library.route) }) {
                        Text("See all")
                    }
                }
            }
        }

        when {
            uiState.isChaptersLoading -> item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            uiState.chapters.isEmpty() -> item {
                Text(
                    text = "No chapters yet. Scan a document to get started!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            else -> items(uiState.chapters, key = { it.id }) { chapter ->
                ChapterCard(
                    chapter = chapter,
                    onClick = { navController.navigate(Routes.ChapterDetail.createRoute(chapter.id)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// Shown at most once per app launch; not persisted so it reappears next open,
// as long as the user hasn't added a personal key.
private var apiKeyPromptShownThisSession = false

@Composable
private fun KeyActiveBadge() {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.22f)
    ) {
        Text(
            text = "🔑",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ApiKeyPromptDialog(
    remainingToday: Int,
    onAddKey: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock unlimited AI", fontWeight = FontWeight.SemiBold) },
        text = {
            Text(
                "You're on the free plan: $remainingToday of ${GenerationQuota.DAILY_FREE_LIMIT} " +
                    "AI generations left today (quizzes, summaries, flashcards).\n\n" +
                    "Add your own free Gemini API key for unlimited generations — it takes " +
                    "about a minute.\n\n" +
                    "For your security, the key is stored only on this device. We never save " +
                    "it in the cloud."
            )
        },
        confirmButton = {
            Button(
                onClick = onAddKey,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Purple,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text("Add my API key", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Not now — use free plan",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
