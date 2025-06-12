package com.example.studymateai.ui.screen.quizz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studymateai.data.model.quizz.QuizQuestion

@Composable
fun QuizQuestionList(questions: List<QuizQuestion>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(questions) { question ->
            QuizQuestionCard(question = question)
        }
    }
}