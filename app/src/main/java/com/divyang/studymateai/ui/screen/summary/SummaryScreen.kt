package com.divyang.studymateai.ui.screen.summary

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.divyang.studymateai.gemini.GeminiClient
import com.divyang.studymateai.ui.components.AppColors
import com.divyang.studymateai.ui.components.AppErrorCard
import com.divyang.studymateai.ui.components.AppTopBar
import com.divyang.studymateai.ui.components.ConfirmationDialog
import com.divyang.studymateai.ui.components.RefreshActionButton
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

    val showRegenerateConfirm = remember { mutableStateOf(false) }

    LaunchedEffect(chapterId) {
        try {
            val document = firestore.collection("chapters")
                .document(chapterId)
                .get()
                .await()

            chapterContent.value = document.getString("content") ?: ""
        } catch (e: Exception) {
            errorState.value = "We couldn't load this chapter's content. Please check your connection and try again."
            Log.e("SummaryScreen", "Error loading content", e)
        } finally {
            isLoadingContent.value = false
        }
    }

    // Generate summary once when content first loads
    LaunchedEffect(chapterContent.value) {
        if (chapterContent.value.isNotEmpty() && summary.value.isEmpty()) {
            coroutineScope.launch {
                generateSummary(
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
            AppTopBar(
                title = "Chapter Summary",
                onBack = { navController.popBackStack() },
                actions = {
                    if (summary.value.isNotEmpty() && !isGeneratingSummary.value) {
                        RefreshActionButton(
                            enabled = chapterContent.value.isNotEmpty(),
                            onClick = { showRegenerateConfirm.value = true }
                        )
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
                    AppErrorCard(
                        message = errorState.value!!,
                        onRetry = {
                            coroutineScope.launch {
                                if (chapterContent.value.isEmpty()) {
                                    isLoadingContent.value = true
                                    errorState.value = null
                                    try {
                                        val document = firestore.collection("chapters")
                                            .document(chapterId)
                                            .get()
                                            .await()
                                        chapterContent.value = document.getString("content") ?: ""
                                    } catch (e: Exception) {
                                        errorState.value = "We couldn't load this chapter's content. Please check your connection and try again."
                                        Log.e("SummaryScreen", "Error loading content", e)
                                    } finally {
                                        isLoadingContent.value = false
                                    }
                                } else {
                                    generateSummary(
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
                                        content = chapterContent.value,
                                        summaryState = summary,
                                        errorState = errorState,
                                        isLoading = isGeneratingSummary
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Purple)
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

            if (showRegenerateConfirm.value) {
                ConfirmationDialog(
                    title = "Regenerate Summary?",
                    message = "This will replace your current summary. This can't be undone.",
                    confirmText = "Regenerate",
                    onConfirm = {
                        showRegenerateConfirm.value = false
                        coroutineScope.launch {
                            generateSummary(
                                content = chapterContent.value,
                                summaryState = summary,
                                errorState = errorState,
                                isLoading = isGeneratingSummary
                            )
                        }
                    },
                    onDismiss = { showRegenerateConfirm.value = false }
                )
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "KEY SUMMARY",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
                color = AppColors.Purple,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
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
        CircularProgressIndicator(color = AppColors.Purple)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message)
    }
}

private suspend fun generateSummary(
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

        val responseText = GeminiClient.generateContent(prompt)
        summaryState.value = responseText.trim().ifEmpty { "No summary generated" }
    } catch (e: Exception) {
        errorState.value = "Gemini couldn't generate a summary right now. This is usually temporary — please try again."
        Log.e("SummaryScreen", "Generation error", e)
    } finally {
        isLoading.value = false
    }
}