package com.example.studymateai.ui.screen.quizz

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.studymateai.BuildConfig
import com.example.studymateai.R
import com.example.studymateai.data.model.quizz.QuizQuestion
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizGenerationScreen(
    navController: NavController,
    chapterId: String
) {
    val firestore = Firebase.firestore
    val chapterContent = remember { mutableStateOf("") }
    val isLoadingContent = remember { mutableStateOf(true) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val isLoadingQuiz = remember { mutableStateOf(false) }
    val quizQuestions = remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }

    // Initialize Gemini
    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    LaunchedEffect(chapterId) {
        try {
            val document = firestore.collection("chapters")
                .document(chapterId)
                .get()
                .await()

            chapterContent.value = document.getString("content") ?: ""
        } catch (e: Exception) {
            errorState.value = "Failed to load chapter content: ${e.localizedMessage}"
            Log.e("QuizGeneration", "Error loading chapter content", e)
        } finally {
            isLoadingContent.value = false
        }
    }

    // Function to generate quiz
    fun generateQuiz() {
        coroutineScope.launch {
            isLoadingQuiz.value = true
            errorState.value = null

            try {
                val prompt = """
                Generate 5 multiple-choice questions from this text: 
                "${chapterContent.value}". 
                Format as a JSON array where each question has:
                {
                    "question": "The question text",
                    "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
                    "correctAnswer": "Correct option text"
                }
                Return ONLY the JSON array with no additional text or markdown formatting.
            """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                Log.d("QuizResponse", "Raw response: ${response.text}")

                val questions = parseQuizResponse(response.text ?: "")
                Log.d("QuizQuestions", "Parsed ${questions.size} questions")

                quizQuestions.value = questions
            } catch (e: Exception) {
                errorState.value = "Failed to generate quiz: ${e.localizedMessage}"
                Log.e("QuizGeneration", "Quiz generation error", e)
            } finally {
                isLoadingQuiz.value = false
            }
        }
    }

    // Auto-generate quiz when content loads
    LaunchedEffect(chapterContent.value) {
        if (chapterContent.value.isNotEmpty()) {
            generateQuiz()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generated Quiz") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isLoadingContent.value && !isLoadingQuiz.value) {
                        IconButton(
                            onClick = { generateQuiz() },
                            enabled = chapterContent.value.isNotEmpty()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.refresh),
                                contentDescription = "Regenerate"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoadingContent.value -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading chapter content...")
                    }
                }

                errorState.value != null -> {
                    ErrorMessage(
                        message = errorState.value!!,
                        onRetry = { generateQuiz() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                isLoadingQuiz.value -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generating quiz questions...")
                    }
                }

                else -> {
                    if (quizQuestions.value.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No questions generated")
                            Button(
                                onClick = { generateQuiz() },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Try Again")
                            }
                        }
                    } else {
                        QuizQuestionList(
                            questions = quizQuestions.value
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}


private fun parseQuizResponse(response: String): List<QuizQuestion> {
    return try {
        val cleanResponse = response
            .replace("```json", "")
            .replace("```", "")
            .trim()

        Log.d("CleanResponse", cleanResponse)
        Gson().fromJson(cleanResponse, Array<QuizQuestion>::class.java).toList()
    } catch (e: Exception) {
        Log.e("ParseError", "Failed to parse: $response", e)
        emptyList()
    }
}