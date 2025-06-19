package com.example.studymateai.ui.screen.quizz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studymateai.data.model.quizz.QuizQuestion

@Composable
fun QuizResultCard(
    questions: List<QuizQuestion>,
    selectedAnswers: Map<Int, String>,
    score: Int,
    onRetake: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Quiz Results",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Score: $score%",
            style = MaterialTheme.typography.displaySmall,
            color = when {
                score >= 80 -> MaterialTheme.colorScheme.primary
                score >= 50 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(questions) { question ->
                val questionIndex = questions.indexOf(question)
                val userAnswer = selectedAnswers[questionIndex]
                val isCorrect = userAnswer == question.correctAnswer

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    )
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons arranged in a row
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
//            Button(
//                onClick = onRetake,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Retake Quiz")
//            }

            Button(
                onClick = onGoHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go Home")
            }
        }
    }
}