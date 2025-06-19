package com.example.studymateai.ui.screen.main

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.studymateai.data.model.chapters.Chapter
import com.example.studymateai.navigation.Routes
import com.example.studymateai.ui.components.BottomNavigationBar
import com.example.studymateai.ui.components.ChapterCard
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController
) {
    val firestore = Firebase.firestore
    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var activeSearch by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }

    // Fetch all chapters ordered by date (newest first)
    LaunchedEffect(Unit) {
        try {
            val userId = Firebase.auth.currentUser?.uid ?: return@LaunchedEffect
            val querySnapshot = firestore.collection("chapters")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            chapters = querySnapshot.documents
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
                .sortedByDescending { it.createdAt } // Newest first
        } catch (e: Exception) {
            Log.e("LibraryScreen", "Error fetching chapters", e)
        } finally {
            isLoading = false
        }
    }

    // Filter chapters based on search query for main list
    val filteredChapters = chapters.filter { chapter ->
        searchQuery.isEmpty() ||
                chapter.title.contains(searchQuery, ignoreCase = true) ||
                chapter.description.contains(searchQuery, ignoreCase = true)
    }

    // Filter for search suggestions (shorter list, more strict matching)
    val searchSuggestions = chapters
        .filter { chapter ->
            searchQuery.isNotEmpty() &&
                    (chapter.title.startsWith(searchQuery, ignoreCase = true) ||
                            chapter.title.contains(searchQuery, ignoreCase = true))
        }
        .take(5)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { if (activeSearch) Text("Search Library") else Text("Your Library") },
                actions = {
                    if (!activeSearch) {
                        IconButton(onClick = {
                            activeSearch = true
                            showSuggestions = true
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding()
                )
        ) {
            if (activeSearch) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        showSuggestions = it.isNotEmpty()
                    },
                    onSearch = { activeSearch = false },
                    active = activeSearch,
                    onActiveChange = { activeSearch = it },
                    modifier = Modifier
                        .fillMaxWidth(),
                    placeholder = { Text("Search chapters...") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                showSuggestions = false
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    }
                ) {
                    // Search suggestions dropdown
                    if (showSuggestions && searchSuggestions.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchSuggestions) { chapter ->
                                ChapterCard(
                                    chapter = chapter,
                                    onClick = {
                                        navController.navigate(
                                            Routes.ChapterDetail.createRoute(chapter.id)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else if (showSuggestions && searchQuery.isNotEmpty()) {
                        Text(
                            text = "No suggestions found",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Main content
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredChapters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty())
                            "No chapters found. Create your first chapter!"
                        else
                            "No matching chapters for \"$searchQuery\"",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = 8.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredChapters) { chapter ->
                        ChapterCard(
                            chapter = chapter,
                            onClick = {
                                navController.navigate(
                                    Routes.ChapterDetail.createRoute(chapter.id)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}