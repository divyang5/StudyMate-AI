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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.divyang.studymateai.R
import com.divyang.studymateai.data.viewmodel.ChapterDetailUiState
import com.divyang.studymateai.data.viewmodel.ChapterDetailViewModel
import com.divyang.studymateai.navigation.Routes
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailScreen(
    navController: NavController,
    chapterId: String,
    viewModel: ChapterDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = (uiState as? ChapterDetailUiState.Success)?.chapter?.title
                                ?: "Chapter Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEEEDFE)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF534AB7),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(6.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        if (uiState is ChapterDetailUiState.Success) {
                            IconButton(onClick = {
                                val chapter = (uiState as ChapterDetailUiState.Success).chapter
                                val encodedTitle       = URLEncoder.encode(chapter.title, "UTF-8")
                                val encodedDescription = URLEncoder.encode(chapter.description, "UTF-8")
                                val encodedContent     = URLEncoder.encode(chapter.content, "UTF-8")
                                navController.navigate(
                                    Routes.TextEdit.createRoute(
                                        title = encodedTitle,
                                        description = encodedDescription,
                                        content = encodedContent,
                                        chapterId = chapter.id
                                    )
                                )
                            }) {
                                Surface(shape = CircleShape, color = Color(0xFFEEEDFE)) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color(0xFF534AB7),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                )
                // ── Gradient strip ─────────────────────────────
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
            when (val state = uiState) {
                is ChapterDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF534AB7)
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
                                    color = Color(0xFFEEEDFE),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = state.chapter.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF3C3489),
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
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "CONTENT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 0.6.sp
                                        ),
                                        color = Color(0xFF534AB7)
                                    )
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = Color(0xFF534AB7).copy(alpha = 0.15f)
                                    )
                                    Text(
                                        text = state.chapter.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 22.sp
                                    )
                                }
                            }

                            // ── Action buttons ─────────────────────────────
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Primary action — filled purple
                                ActionButton(
                                    iconResId = R.drawable.quizz,
                                    text = "Make Quiz",
                                    containerColor = Color(0xFF534AB7),
                                    contentColor = Color.White,
                                    onClick = {
                                        navController.navigate(
                                            Routes.QuizGen.createRoute(chapterId = chapterId)
                                        )
                                    }
                                )
                                // Secondary actions — tinted surfaces
                                ActionButton(
                                    iconResId = R.drawable.library,
                                    text = "Generate Summary",
                                    containerColor = Color(0xFFEEEDFE),
                                    contentColor = Color(0xFF3C3489),
                                    onClick = {
                                        navController.navigate(Routes.Summary.createRoute(chapterId))
                                    }
                                )
                                ActionButton(
                                    iconResId = R.drawable.flashcard,
                                    text = "Create Flashcards",
                                    containerColor = Color(0xFFE1F5EE),
                                    contentColor = Color(0xFF0F6E56),
                                    onClick = {
                                        navController.navigate(Routes.Flashcards.createRoute(chapterId))
                                    }
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
fun ActionButton(
    iconResId: Int,
    text: String,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor   = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = text,
                modifier = Modifier.size(20.dp)
            )
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}