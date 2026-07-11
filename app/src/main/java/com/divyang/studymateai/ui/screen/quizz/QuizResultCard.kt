package com.divyang.studymateai.ui.screen.quizz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.divyang.studymateai.R
import com.divyang.studymateai.data.model.quizz.QuizQuestion
import com.divyang.studymateai.ui.components.PrimaryButton

@Composable
fun QuizResultCard(
    questions: List<QuizQuestion>,
    selectedAnswers: Map<Int, String>,
    score: Int,
    onRetake: () -> Unit,
    onGoHome: () -> Unit
) {
    val correctCount = questions.indices.count { i ->
        selectedAnswers[i] == questions[i].correctAnswer
    }
    val wrongCount = questions.size - correctCount

    val gradeColor = when {
        score >= 80 -> MaterialTheme.colorScheme.tertiary
        score >= 50 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    val gradeLabel = when {
        score >= 80 -> "Excellent! 🎉"
        score >= 50 -> "Nice work! 👏"
        else -> "Keep practicing 💪"
    }

    // Animate the ring from 0 → score once on entry.
    var played by remember { mutableStateOf(false) }
    val ringProgress by animateFloatAsState(
        targetValue = if (played) score / 100f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "scoreRing"
    )
    LaunchedEffect(Unit) { played = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Hero: animated score ring ───────────────────────────────
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(150.dp),
                color = gradeColor.copy(alpha = 0.15f),
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round
            )
            CircularProgressIndicator(
                progress = { ringProgress },
                modifier = Modifier.size(150.dp),
                color = gradeColor,
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = gradeColor
                )
                Text(
                    text = "$correctCount of ${questions.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = gradeLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(12.dp))

        // ── Correct / wrong stat chips ──────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultStatChip(
                iconRes = R.drawable.passed,
                label = "$correctCount correct",
                color = MaterialTheme.colorScheme.tertiary
            )
            ResultStatChip(
                iconRes = R.drawable.close,
                label = "$wrongCount wrong",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Review list ─────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(questions) { index, question ->
                val userAnswer = selectedAnswers[index]
                val isCorrect = userAnswer == question.correctAnswer
                ReviewCard(
                    number = index + 1,
                    question = question,
                    userAnswer = userAnswer,
                    isCorrect = isCorrect
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        PrimaryButton(text = "Go Home", onClick = onGoHome)
    }
}

@Composable
private fun ResultStatChip(iconRes: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun ReviewCard(
    number: Int,
    question: QuizQuestion,
    userAnswer: String?,
    isCorrect: Boolean
) {
    val statusColor = if (isCorrect) MaterialTheme.colorScheme.tertiary
    else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            // Status badge
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(statusColor.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isCorrect) R.drawable.passed else R.drawable.close),
                    contentDescription = if (isCorrect) "Correct" else "Wrong",
                    tint = statusColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Q$number. ${question.question}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Your answer: ${userAnswer ?: "Not answered"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
                if (!isCorrect) {
                    Text(
                        text = "Correct answer: ${question.correctAnswer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}
