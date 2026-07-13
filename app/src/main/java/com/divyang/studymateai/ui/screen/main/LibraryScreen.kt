package com.divyang.studymateai.ui.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.divyang.studymateai.data.model.chapters.Chapter
import com.divyang.studymateai.data.viewmodel.LibraryViewModel
import com.divyang.studymateai.data.viewmodel.filteredChapters
import com.divyang.studymateai.navigation.Routes
import com.divyang.studymateai.ui.components.BottomNavigationBar
import com.divyang.studymateai.ui.components.ChapterCard
import com.divyang.studymateai.ui.components.DeleteConfirmDialog
import com.divyang.studymateai.ui.components.GradientHero
import com.divyang.studymateai.ui.components.SwipeToDeleteContainer
import com.divyang.studymateai.ui.components.verticalScrollbar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredChapters = uiState.filteredChapters
    var chapterToDelete by remember { mutableStateOf<Chapter?>(null) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = viewModel::refresh
    )

    val count = uiState.chapters.size
    val subtitle = if (count == 1) "1 saved chapter" else "$count saved chapters"

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            Column {
                GradientHero(title = "Your Library", subtitle = subtitle)

                SearchField(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )

                when {
                    uiState.isLoading -> LoadingState()
                    filteredChapters.isEmpty() -> EmptyState(searchQuery = uiState.searchQuery)
                    else -> ChapterList(
                        chapters = filteredChapters,
                        onChapterClick = { navController.navigate(Routes.ChapterDetail.createRoute(it.id)) },
                        onDeleteRequest = { chapterToDelete = it }
                    )
                }
            }

            PullRefreshIndicator(
                refreshing = uiState.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    chapterToDelete?.let { chapter ->
        DeleteConfirmDialog(
            title = "Delete chapter?",
            message = "\"${chapter.title}\" will be permanently removed.",
            onConfirm = {
                viewModel.deleteChapter(chapter.id)
                chapterToDelete = null
            },
            onDismiss = { chapterToDelete = null }
        )
    }
}
// ─── Sub-composables ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search chapters...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large
    )
}

@Composable
private fun ChapterList(
    chapters: List<Chapter>,
    onChapterClick: (Chapter) -> Unit,
    onDeleteRequest: (Chapter) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.verticalScrollbar(listState),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(chapters, key = { it.id }) { chapter ->
            SwipeToDeleteContainer(
                onDelete = { onDeleteRequest(chapter) }
            ) {
                ChapterCard(
                    chapter = chapter,
                    onClick = { onChapterClick(chapter) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier.fillParentMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun EmptyState(searchQuery: String) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier.fillParentMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isEmpty())
                        "No chapters yet. Create your first one!"
                    else
                        "No results for \"$searchQuery\"",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
