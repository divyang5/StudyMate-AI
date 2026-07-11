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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.divyang.studymateai.R
import com.divyang.studymateai.ads.AdManager
import com.divyang.studymateai.data.viewmodel.HomeUiState
import com.divyang.studymateai.data.viewmodel.HomeViewModel
import com.divyang.studymateai.navigation.Routes
import com.divyang.studymateai.ui.components.BottomNavigationBar
import com.divyang.studymateai.ui.components.ChapterCard
import com.divyang.studymateai.ui.components.GlassActionTile
import com.divyang.studymateai.ui.components.GradientHero
import com.divyang.studymateai.ui.components.HeroStatPill
import com.divyang.studymateai.ui.components.SectionHeading
import java.util.Calendar

// ─── Screen ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val adManager = remember { AdManager(context) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = viewModel::refresh
    )

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
            HomeContent(uiState = uiState, navController = navController, adManager = adManager)

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

    LazyColumn(
        state = rememberLazyListState(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Hero ───────────────────────────────────────────────────────────
        item {
            GradientHero(
                title = greeting,
                subtitle = "Ready to learn something new?",
                stats = {
                    HeroStatPill(value = "${uiState.chapters.size}", label = "Recent chapters")
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
                        onClick = { navController.navigate(Routes.TextEdit.createRoute("", "", "")) },
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
