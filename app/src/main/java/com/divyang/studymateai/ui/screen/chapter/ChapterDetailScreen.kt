package com.divyang.studymateai.ui.screen.chapter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.divyang.studymateai.R
import com.divyang.studymateai.data.viewmodel.ChapterDetailUiState
import com.divyang.studymateai.data.viewmodel.ChapterDetailViewModel
import com.divyang.studymateai.navigation.Routes
import com.divyang.studymateai.ui.components.AppColors
import com.divyang.studymateai.ui.components.AppTopBar
import com.divyang.studymateai.ui.components.GlassActionTile
import com.divyang.studymateai.ui.components.verticalScrollbar
import com.divyang.studymateai.utils.TextBlocks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailScreen(
    navController: NavController,
    chapterId: String,
    savedStateHandle: SavedStateHandle? = null,
    viewModel: ChapterDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // The editor sets this flag on save so we reload instead of showing the
    // pre-edit chapter when the user navigates back.
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow(Routes.KEY_CHAPTER_CHANGED, false)?.collect { changed ->
            if (changed) {
                savedStateHandle[Routes.KEY_CHAPTER_CHANGED] = false
                viewModel.loadChapter()
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = (uiState as? ChapterDetailUiState.Success)?.chapter?.title ?: "Chapter Details",
                onBack = { navController.popBackStack() },
                actions = {
                    if (uiState is ChapterDetailUiState.Success) {
                        IconButton(onClick = {
                            // Only the id travels through the route — the
                            // editor loads the chapter itself. Encoding the
                            // content into the route crashed route matching.
                            navController.navigate(
                                Routes.TextEdit.createRoute(chapterId = chapterId)
                            )
                        }) {
                            Surface(shape = CircleShape, color = AppColors.PurpleTint) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = AppColors.Purple,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(6.dp)
                                )
                            }
                        }
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
            when (val state = uiState) {
                is ChapterDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is ChapterDetailUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Couldn't load chapter",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.loadChapter() },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }

                is ChapterDetailUiState.Success -> {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ── Description chip ───────────────────────────
                            if (state.chapter.description.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = state.chapter.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }
                            }

                            // ── Content card ───────────────────────────────
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                // Long chapters as one Text meant the whole
                                // string was laid out at once; a LazyColumn of
                                // paragraph blocks only composes the viewport.
                                val contentBlocks = remember(state.chapter.content) {
                                    TextBlocks.split(state.chapter.content)
                                }
                                val contentListState = rememberLazyListState()
                                LazyColumn(
                                    state = contentListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScrollbar(contentListState),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "CONTENT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                letterSpacing = 0.6.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    item {
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    }
                                    items(contentBlocks) { block ->
                                        Text(
                                            text = block,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }

                            // ── Actions — icon-forward glassy tiles, one row,
                            // leaving the vertical space to the content ─────
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassActionTile(
                                    iconRes = R.drawable.quizz,
                                    label = "Quiz",
                                    subtitle = "Test yourself",
                                    onClick = {
                                        navController.navigate(
                                            Routes.QuizGen.createRoute(chapterId = chapterId)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                GlassActionTile(
                                    iconRes = R.drawable.library,
                                    label = "Summary",
                                    subtitle = "Key points",
                                    onClick = {
                                        navController.navigate(Routes.Summary.createRoute(chapterId))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                GlassActionTile(
                                    iconRes = R.drawable.flashcard,
                                    label = "Cards",
                                    subtitle = "Flashcards",
                                    onClick = {
                                        navController.navigate(Routes.Flashcards.createRoute(chapterId))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

