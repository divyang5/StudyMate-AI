package com.example.studymateai.ui.screen.quizz

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.studymateai.data.model.quizz.QuizQuestion
import com.example.studymateai.navigation.Routes
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizHistoryDetailScreen(
    navController: NavController,
    quizHistoryId: String
) {
    val auth = Firebase.auth
    val firestore = Firebase.firestore
    val isLoading = remember { mutableStateOf(true) }
    val quizQuestions = remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    val selectedAnswers = remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    val score = remember { mutableStateOf(0) }
    val chapterTitle = remember { mutableStateOf("") }
    val quizDate = remember { mutableStateOf<Date?>(null) }

    LaunchedEffect(quizHistoryId) {
        try {
            val doc = firestore.collection("quizHistory").document(quizHistoryId).get().await()

            // Get score, date and chapter title
            score.value = doc.getLong("score")?.toInt() ?: 0
            val chapterId = doc.getString("chapterId") ?: ""
            quizDate.value = doc.getDate("date")

            // Fetch chapter title
            val chapterDoc = firestore.collection("chapters").document(chapterId).get().await()
            chapterTitle.value = chapterDoc.getString("title") ?: "Unknown Chapter"

            // Get questions and answers
            val questionsList = mutableListOf<QuizQuestion>()
            val answersMap = mutableMapOf<Int, String>()

            val questionsData = doc.get("questions") as? List<Map<String, String>> ?: emptyList()
            questionsData.forEachIndexed { index, questionData ->
                questionsList.add(
                    QuizQuestion(
                        question = questionData["question"] ?: "",
                        correctAnswer = questionData["correctAnswer"] ?: "",
                        options = emptyList() // Not stored in history
                    )
                )
                answersMap[index] = questionData["userAnswer"] ?: ""
            }

            quizQuestions.value = questionsList
            selectedAnswers.value = answersMap
        } catch (e: Exception) {
            Log.e("QuizHistoryDetail", "Error fetching quiz details", e)
        } finally {
            isLoading.value = false
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Quiz Results") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading.value) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = chapterTitle.value,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Date: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(quizDate.value ?: Date())}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Your Score: ${score.value}%",
                                style = MaterialTheme.typography.displaySmall,
                                color = when {
                                    score.value >= 80 -> MaterialTheme.colorScheme.primary
                                    score.value >= 50 -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }

                    items(quizQuestions.value) { question ->
                        val questionIndex = quizQuestions.value.indexOf(question)
                        val userAnswer = selectedAnswers.value[questionIndex]
                        val isCorrect = userAnswer == question.correctAnswer

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrect) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.errorContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = question.question,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your answer: ${userAnswer ?: "Not answered"}",
                                    color = if (isCorrect) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Correct answer: ${question.correctAnswer}",
                                    color = if (isCorrect) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { navController.popBackStack(Routes.Home.route, false) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Go Home")
                            }
                        }
                    }
                }
            }
        }
    }
}