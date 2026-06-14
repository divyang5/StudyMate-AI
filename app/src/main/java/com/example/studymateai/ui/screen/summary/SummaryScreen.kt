package com.example.studymateai.ui.screen.summary

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.studymateai.BuildConfig
import com.example.studymateai.R
import com.example.studymateai.ui.screen.quizz.ErrorMessage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    navController: NavController,
    chapterId: String
) {
    val firestore = Firebase.firestore
    val chapterContent = remember { mutableStateOf("") }
    val isLoadingContent = remember { mutableStateOf(true) }
    val isGeneratingSummary = remember { mutableStateOf(false) }
    val summary = remember { mutableStateOf("") }
    val errorState = remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
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
            Log.e("SummaryScreen", "Error loading content", e)
        } finally {
            isLoadingContent.value = false
        }
    }

    // Generate summary when content loads
    LaunchedEffect(chapterContent.value) {
        if (chapterContent.value.isNotEmpty()) {
            coroutineScope.launch {
                generateSummary(
                    generativeModel = generativeModel,
                    content = chapterContent.value,
                    summaryState = summary,
                    errorState = errorState,
                    isLoading = isGeneratingSummary
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chapter Summary") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (summary.value.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    generateSummary(
                                        generativeModel = generativeModel,
                                        content = chapterContent.value,
                                        summaryState = summary,
                                        errorState = errorState,
                                        isLoading = isGeneratingSummary
                                    )
                                }
                            }
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
                    LoadingState(message = "Loading chapter content...")
                }

                isGeneratingSummary.value -> {
                    LoadingState(message = "Generating summary...")
                }

                errorState.value != null -> {
                    ErrorMessage(
                        message = errorState.value!!,
                        onRetry = {
                            coroutineScope.launch {
                                if (chapterContent.value.isEmpty()) {
                                    // Retry content loading
                                    isLoadingContent.value = true
                                    errorState.value = null
                                    val document = firestore.collection("chapters")
                                        .document(chapterId)
                                        .get()
                                        .await()
                                    chapterContent.value = document.getString("content") ?: ""
                                } else {
                                    generateSummary(
                                        generativeModel = generativeModel,
                                        content = chapterContent.value,
                                        summaryState = summary,
                                        errorState = errorState,
                                        isLoading = isGeneratingSummary
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                summary.value.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No summary available")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    generateSummary(
                                        generativeModel = generativeModel,
                                        content = chapterContent.value,
                                        summaryState = summary,
                                        errorState = errorState,
                                        isLoading = isGeneratingSummary
                                    )
                                }
                            }
                        ) {
                            Text("Generate Summary")
                        }
                    }
                }

                else -> {
                    SummaryContent(
                        summary = summary.value,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}
@Composable
fun SummaryContent(
    summary: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Key Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
@Composable
fun LoadingState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(message)
    }
}

private suspend fun generateSummary(
    generativeModel: GenerativeModel,
    content: String,
    summaryState: MutableState<String>,
    errorState: MutableState<String?>,
    isLoading: MutableState<Boolean>
) {
    isLoading.value = true
    errorState.value = null

    try {
        val prompt = """
            Summarize this text in short concise bullet points:
            "$content"
            
            Requirements:
            - Use markdown formatting with bullet points
            - Each point should be 1-2 sentences
            - Focus on key concepts
            - Skip introductions
            - Return only the bullet points
            - give some space between points
            
            Example format:
            - First key point
            - Second important concept
            - Third main idea
        """.trimIndent()

        val response = generativeModel.generateContent(prompt)
        summaryState.value = response.text?.trim() ?: "No summary generated"
    } catch (e: Exception) {
        errorState.value = "Failed to generate summary: ${e.localizedMessage}"
        Log.e("SummaryScreen", "Generation error", e)
    } finally {
        isLoading.value = false
    }
}

