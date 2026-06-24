package com.example.studymateai.ui.screen.quizz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studymateai.R
import com.example.studymateai.data.model.quizz.QuizHistory
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun QuizHistoryCard(
    history: QuizHistory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(history.date)
    val passed = history.score >= 50

    val accentGradient = if (passed) {
        Brush.horizontalGradient(listOf(Color(0xFF534AB7), Color(0xFF1D9E75)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFA32D2D), Color(0xFFD85A30)))
    }

    val chipBg        = if (passed) Color(0xFFE1F5EE) else Color(0xFFFCEBEB)
    val chipText      = if (passed) Color(0xFF0F6E56)  else Color(0xFFA32D2D)
    val scoreColor    = if (passed) Color(0xFF0F6E56)  else Color(0xFFA32D2D)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor   = MaterialTheme.colorScheme.onSurface
        ),
        border   = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier.fillMaxWidth()
    ) {
        // ── Accent gradient strip ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(accentGradient)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)
        ) {
            // ── Pass/Fail chip ─────────────────────────────────
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = chipBg
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier              = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        painter           = painterResource(if (passed) R.drawable.passed else R.drawable.close),
                        contentDescription = null,
                        tint              = chipText,
                        modifier          = Modifier.size(13.dp)
                    )
                    Text(
                        text  = if (passed) "Passed" else "Failed",
                        style = MaterialTheme.typography.labelSmall,
                        color = chipText
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Title + score badge ────────────────────────────
            Row(
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text(
                    text       = history.chapterTitle,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = chipBg,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text  = "${history.score}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = scoreColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Score value ────────────────────────────────────
            Text(
                text  = "Score",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Text(
                text  = "${history.score} / 100",
                style = MaterialTheme.typography.titleMedium,
                color = scoreColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )

            // ── Divider ───────────────────────────────────────
            HorizontalDivider(
                modifier  = Modifier.padding(vertical = 10.dp),
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            // ── Footer: date ──────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter           = painterResource(R.drawable.ic_clock),
                    contentDescription = null,
                    tint              = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier          = Modifier.size(13.dp)
                )
                Text(
                    text  = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}