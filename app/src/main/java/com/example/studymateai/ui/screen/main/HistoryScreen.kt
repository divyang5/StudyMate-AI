package com.example.studymateai.ui.screen.main

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.studymateai.data.model.quizz.QuizHistory
import com.example.studymateai.navigation.Routes
import com.example.studymateai.ui.components.BottomNavigationBar
import com.example.studymateai.ui.screen.quizz.QuizHistoryCard
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HistoryScreen(
    navController: NavController
) {
    val auth = Firebase.auth
    val firestore = Firebase.firestore
    val isLoading = remember { mutableStateOf(true) }
    val quizHistory = remember { mutableStateOf<List<QuizHistory>>(emptyList()) }
    val chapterTitles = remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var historyToDelete by remember { mutableStateOf<QuizHistory?>(null) }

    // Function to delete quiz history
    fun deleteHistory(historyId: String) {
        firestore.collection("quizHistory").document(historyId)
            .delete()
            .addOnSuccessListener {
                quizHistory.value = quizHistory.value.filter { it.id != historyId }
                Log.d("HistoryScreen", "Quiz history deleted successfully")
            }
            .addOnFailureListener { e ->
                Log.e("HistoryScreen", "Error deleting quiz history", e)
            }
    }

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid ?: return@LaunchedEffect
        try {
            // Get quiz history
            val querySnapshot = firestore.collection("quizHistory")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // Get all unique chapter IDs
            val chapterIds = querySnapshot.documents.mapNotNull { it.getString("chapterId") }.toSet()

            // Fetch chapter titles
            val titlesMap = mutableMapOf<String, String>()
            chapterIds.forEach { chapterId ->
                val chapterDoc = firestore.collection("chapters").document(chapterId).get().await()
                titlesMap[chapterId] = chapterDoc.getString("title") ?: "Unknown Chapter"
            }
            chapterTitles.value = titlesMap

            // Map to QuizHistory objects
            val historyList = querySnapshot.documents.mapNotNull { doc ->
                val date = doc.getDate("date") ?: return@mapNotNull null
                QuizHistory(
                    id = doc.id,
                    chapterId = doc.getString("chapterId") ?: "",
                    score = doc.getLong("score")?.toInt() ?: 0,
                    date = date,
                    chapterTitle = titlesMap[doc.getString("chapterId")] ?: "Unknown Chapter"
                )
            }.sortedByDescending { it.date }

            quizHistory.value = historyList
        } catch (e: Exception) {
            Log.e("HistoryScreen", "Error fetching quiz history", e)
        } finally {
            isLoading.value = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz History") },
                actions = {}
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (quizHistory.value.isEmpty()) {
                Text(
                    text = "No quiz history found",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quizHistory.value) { history ->
                        SwipeToDeleteContainer(
                            item = history,
                            onDelete = {
                                historyToDelete = history
                                showDeleteDialog = true
                            }
                        ) {
                            QuizHistoryCard(
                                history = history,
                                onClick = {
                                    navController.navigate(Routes.QuizHistoryDetail.createRoute(history.id))
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && historyToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Quiz History") },
            text = { Text("Are you sure you want to delete this quiz history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        historyToDelete?.id?.let { deleteHistory(it) }
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}