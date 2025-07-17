package com.example.studymateai.ui.screen.flashCard

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.studymateai.BuildConfig
import com.example.studymateai.data.model.flashCard.Flashcard
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.VerticalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashCardScreen(
    navController: NavController,
    chapterId: String
) {
    val firestore = Firebase.firestore
    val chapterContent = remember { mutableStateOf("") }
    val isLoadingContent = remember { mutableStateOf(true) }
    val errorState = remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val isLoadingFlashcards = remember { mutableStateOf(false) }
    val flashcards = remember { mutableStateOf<List<Flashcard>>(emptyList()) }

    // Initialize Gemini
    val generativeModel = remember {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }
    // Define generateFlashcards function before LaunchedEffect
    val generateFlashcards: () -> Unit = {
        coroutineScope.launch {
            isLoadingFlashcards.value = true
            errorState.value = null

            try {
                val prompt = """
                Analyze the following text and generate concise flashcards that summarize all the key points:
                "${chapterContent.value}".
                
                Each flashcard should have:
                - A term/concept (short and clear)
                - A definition/explanation (concise but comprehensive enough to understand the concept , slightly detailed )
                
                Format as a JSON array where each flashcard has:
                {
                    "term": "The term or concept",
                    "definition": "The definition or explanation"
                }
                
                The flashcards should cover all the important concepts from the text in a way that
                someone could understand the whole chapter by studying just these flashcards.
                Generate as many flashcards as needed to properly cover the material - don't limit
                the number, but keep each flashcard focused on one key concept.
                Return ONLY the JSON array with no additional text or markdown formatting.
            """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                Log.d("FlashcardResponse", "Raw response: ${response.text}")

                val generatedFlashcards = parseFlashcardResponse(response.text ?: "")
                Log.d("Flashcards", "Parsed ${generatedFlashcards.size} flashcards")

                flashcards.value = generatedFlashcards
            } catch (e: Exception) {
                errorState.value = "Failed to generate flashcards: ${e.localizedMessage}"
                Log.e("FlashcardGeneration", "Flashcard generation error", e)
            } finally {
                isLoadingFlashcards.value = false
            }
        }
    }

    LaunchedEffect(chapterId) {
        try {
            val document = firestore.collection("chapters")
                .document(chapterId)
                .get()
                .await()

            chapterContent.value = document.getString("content") ?: ""

            generateFlashcards()
        } catch (e: Exception) {
            errorState.value = "Failed to load chapter content: ${e.localizedMessage}"
            Log.e("FlashcardGeneration", "Error loading chapter content", e)
        } finally {
            isLoadingContent.value = false
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chapter Flashcards") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isLoadingContent.value && !isLoadingFlashcards.value) {
                        IconButton(
                            onClick = { generateFlashcards() },
                            enabled = chapterContent.value.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
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
                isLoadingContent.value || isLoadingFlashcards.value -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isLoadingContent.value)
                                "Loading chapter content..."
                            else
                                "Generating flashcards..."
                        )
                    }
                }

                errorState.value != null -> {
                    ErrorMessage(
                        message = errorState.value!!,
                        onRetry = { generateFlashcards() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                flashcards.value.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No flashcards generated")
                        Button(
                            onClick = { generateFlashcards() },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Try Again")
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        SwipeableFlashcards(flashcards = flashcards.value)
                    }
                }
            }
        }
    }
}



private fun parseFlashcardResponse(response: String): List<Flashcard> {
    return try {
        val cleanResponse = response
            .replace("```json", "")
            .replace("```", "")
            .trim()

        Log.d("CleanResponse", cleanResponse)
        Gson().fromJson(cleanResponse, Array<Flashcard>::class.java).toList()
    } catch (e: Exception) {
        Log.e("ParseError", "Failed to parse: $response", e)
        emptyList()
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

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SwipeableFlashcards(flashcards: List<Flashcard>) {
    val pagerState = rememberPagerState()

    VerticalPager(
        count = flashcards.size,
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            FlashcardItem(
                term = flashcards[page].term,
                definition = flashcards[page].definition,
                index = page + 1,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // Add floating page indicator
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
//            .align(Alignment.BottomCenter)
    ) {
        PageIndicatorDots(
            pageCount = flashcards.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun PageIndicatorDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(2.dp)
                    .background(
                        color = if (currentPage == index)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}