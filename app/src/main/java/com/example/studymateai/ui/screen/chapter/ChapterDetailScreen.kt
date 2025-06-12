package com.example.studymateai.ui.screen.chapter

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.studymateai.R
import com.example.studymateai.data.model.chapters.Chapter
import com.example.studymateai.navigation.Routes
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailScreen(
    navController: NavController,
    chapterId: String
) {
    val firestore = Firebase.firestore
    val chapter = remember { mutableStateOf<Chapter?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(chapterId) {
        try {
            val document = firestore.collection("chapters").document(chapterId).get().await()
            chapter.value = Chapter(
                id = document.id,
                title = document.getString("title") ?: "",
                description = document.getString("description") ?: "",
                content = document.getString("content") ?: "",
                createdAt = document.getDate("createdAt") ?: Date()
            )
        } catch (e: Exception) {
            Log.e("ChapterDetail", "Error loading chapter", e)
        } finally {
            isLoading.value = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(chapter.value?.title ?: "Chapter Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
//                        navController.navigate("editChapter/${chapterId}")
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (chapter.value == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chapter not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Content",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = chapter.value!!.content,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }


                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionButton(
                        iconResId = R.drawable.quizz,
                        text = "Make Quiz",
                        onClick = {
                            navController.navigate(
                                Routes.QuizGen.createRoute(chapterId = chapterId)
                            )
                        }
                    )

                    ActionButton(
                        iconResId = R.drawable.library,
                        text = "Generate Summary",
                        onClick = { navController.navigate(Routes.Summary.createRoute(chapterId)) }
                    )

                    ActionButton(
                        iconResId = R.drawable.flashcard,
                        text = "Create Flashcards",
                        onClick = { navController.navigate(Routes.Flashcards.createRoute(chapterId)) }
                    )
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
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = text,
                modifier = Modifier.size(24.dp) // Adjust size as needed
            )
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}