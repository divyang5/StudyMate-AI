package com.example.studymateai.ui.screen.quizz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.studymateai.data.model.quizz.QuizQuestion


@Composable
fun QuizQuestionCard(
    question: QuizQuestion,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit,
    showCorrectAnswer: Boolean,
    modifier: Modifier = Modifier,
    randomColor: Color
) {


    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = randomColor
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                question.options.forEach { option ->
                    val isSelected = selectedAnswer == option
                    val isCorrect = option == question.correctAnswer

                    OutlinedButton(
                        onClick = { onAnswerSelected(option) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = when {
                                showCorrectAnswer && isCorrect -> MaterialTheme.colorScheme.primaryContainer
                                showCorrectAnswer && isSelected && !isCorrect -> MaterialTheme.colorScheme.errorContainer
                                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            },
                            contentColor = when {
                                showCorrectAnswer && isCorrect -> MaterialTheme.colorScheme.onPrimaryContainer
                                showCorrectAnswer && isSelected && !isCorrect -> MaterialTheme.colorScheme.onErrorContainer
                                isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}