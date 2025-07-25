package com.example.studymateai.ui.screen.quizz

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.navigation.NavController
import com.example.studymateai.BuildConfig
import com.example.studymateai.R
import com.example.studymateai.data.model.quizz.QuizQuestion
import com.example.studymateai.navigation.Routes
import com.example.studymateai.ui.screen.flashCard.ErrorMessage
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
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

    val selectedAnswers = remember { mutableStateMapOf<Int, String>() }
    val isSubmitted = remember { mutableStateOf(false) }
    val score = remember { mutableStateOf(0) }
    val showResult = remember { mutableStateOf(false) }
    val view = LocalView.current

    val showQuestionCountDialog = remember { mutableStateOf(true) }
    val questionCount = remember { mutableStateOf(10) }
    val pagerState = rememberPagerState()


    // Initialize Gemini
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
            Log.e("QuizGeneration", "Error loading chapter content", e)
        } finally {
            isLoadingContent.value = false
        }
    }

    // Function to generate quiz
    fun generateQuiz() {
        coroutineScope.launch {
            isLoadingQuiz.value = true  // This should show the loading screen
            errorState.value = null
            showQuestionCountDialog.value = false  // This closes the dialog

            try {
                val prompt = """
                Generate ${questionCount.value} multiple-choice questions from this text: 
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
                showQuestionCountDialog.value = false
            } catch (e: Exception) {
                errorState.value = "Failed to generate quiz: ${e.localizedMessage}"
                Log.e("QuizGeneration", "Quiz generation error", e)
            } finally {
                isLoadingQuiz.value = false
            }
        }
    }



    fun selectAnswer(questionIndex: Int, selectedOption: String) {
        if (!isSubmitted.value) {
            selectedAnswers[questionIndex] = selectedOption
            view.performHapticFeedback(HapticFeedbackConstantsCompat.CONTEXT_CLICK)
        }
    }

    // Function to calculate score
    fun calculateScore(): Int {
        var correct = 0
        quizQuestions.value.forEachIndexed { index, question ->
            if (selectedAnswers[index] == question.correctAnswer) {
                correct++
            }
        }
        return (correct.toFloat() / quizQuestions.value.size * 100).toInt()
    }

    fun saveQuizHistory() {
        coroutineScope.launch {
            try {
                val userId = Firebase.auth.currentUser?.uid ?: return@launch
                val quizData = hashMapOf(
                    "userId" to userId,
                    "chapterId" to chapterId,
                    "score" to score.value,
                    "date" to com.google.firebase.Timestamp.now(),
                    "questions" to quizQuestions.value.map { question ->
                        hashMapOf(
                            "question" to question.question,
                            "correctAnswer" to question.correctAnswer,
                            "userAnswer" to selectedAnswers[quizQuestions.value.indexOf(question)]
                        )
                    }
                )

                Firebase.firestore.collection("quizHistory")
                    .add(quizData)
                    .await()

                Log.d("QuizHistory", "Quiz results saved successfully")
            } catch (e: Exception) {
                Log.e("QuizHistory", "Error saving quiz results", e)
            }
        }
    }

    // Function to submit quiz
    fun submitQuiz() {
        if (selectedAnswers.size == quizQuestions.value.size) {
            isSubmitted.value = true
            score.value = calculateScore()
            showResult.value = true
            saveQuizHistory()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showResult.value) "Quiz Results" else "Question ${pagerState.currentPage + 1}/${quizQuestions.value.size}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isLoadingContent.value && !isLoadingQuiz.value && !showResult.value && !showQuestionCountDialog.value) {
                        IconButton(
                            onClick = { showQuestionCountDialog.value = true },
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

                showQuestionCountDialog.value -> {
                    QuestionCountDialog(
                        minQuestions = 5,
                        maxQuestions = 20,
                        initialCount = questionCount.value,
                        onCountSelected = { count ->
                            questionCount.value = count
                            generateQuiz()
                        },
                        onDismiss = { navController.popBackStack() },
                        isLoading = isLoadingQuiz.value
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
                        Text("Generating ${questionCount.value} quiz questions...")
                    }
                }

                showResult.value -> {
                    QuizResultCard(
                        questions = quizQuestions.value,
                        selectedAnswers = selectedAnswers,
                        score = score.value,
                        onRetake = {
                            selectedAnswers.clear()
                            isSubmitted.value = false
                            showResult.value = false
                            showQuestionCountDialog.value = true
                        },
                        onGoHome = {
                            selectedAnswers.clear()
                            isSubmitted.value = false
                            showResult.value = false
                            navController.navigate(Routes.Home.route)
                        }
                    )
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
                                onClick = { showQuestionCountDialog.value = true },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Try Again")
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                HorizontalPager(
                                    count = quizQuestions.value.size,
                                    state = pagerState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) { page ->
                                    val question = quizQuestions.value[page]
                                    val randomColor = remember(page) {
                                        val hue = Random.nextFloat() * 360f
                                        Color.hsv(
                                            hue = hue,
                                            saturation = 0.7f,
                                            value = 0.8f,
                                            alpha = 0.05f
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(randomColor)
                                    ) {
                                        QuizQuestionCard(
                                            question = question,
                                            selectedAnswer = selectedAnswers[page],
                                            onAnswerSelected = { answer ->
                                                selectAnswer(page, answer)
                                            },
                                            showCorrectAnswer = isSubmitted.value,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.Center)
                                                .padding(16.dp)
                                            ,
                                            randomColor
                                        )
                                    }
                                }

                                Button(
                                    onClick = { submitQuiz() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    enabled = selectedAnswers.size == quizQuestions.value.size && !isSubmitted.value
                                ) {
                                    Text("Submit Quiz")
                                }
                            }
                        }
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

@Composable
fun QuestionCountDialog(
    minQuestions: Int = 5,
    maxQuestions: Int = 20,
    initialCount: Int,
    onCountSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    var sliderValue by remember { mutableStateOf(initialCount.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Number of Questions") },
        text = {
            Column {
                Text("How many questions would you like to generate?")
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { newValue ->
                            sliderValue = newValue
                        },
                        valueRange = minQuestions.toFloat()..maxQuestions.toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$minQuestions")
                        Text(
                            text = "${sliderValue.toInt()} questions",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("$maxQuestions")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCountSelected(sliderValue.toInt())
                },
                enabled = !isLoading
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

