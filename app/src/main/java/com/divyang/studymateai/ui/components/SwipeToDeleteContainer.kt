package com.divyang.studymateai.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val DELETE_THRESHOLD_DP = 96.dp

@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    // Track raw progress separately so the bg composable recomposes on drag
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val thresholdPx = with(LocalDensity.current) { DELETE_THRESHOLD_DP.toPx() }

    val snapBack = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // ── Only paint the delete bg once the user actually starts swiping ──
        if (dragProgress > 0f) {
            DeleteReveal(progress = dragProgress)
        }

        // ── Draggable card ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .zIndex(1f)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value <= -thresholdPx) onDelete()
                                offsetX.animateTo(0f, snapBack)
                                dragProgress = 0f   // hide bg after snap-back
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(0f, snapBack)
                                dragProgress = 0f
                            }
                        },
                        onHorizontalDrag = { _, delta ->
                            scope.launch {
                                val next = (offsetX.value + delta)
                                    .coerceIn(-thresholdPx * 1.25f, 0f)
                                offsetX.snapTo(next)
                                // update progress so the bg recomposes with
                                // the right alpha / scale
                                dragProgress = (-next / thresholdPx).coerceIn(0f, 1f)
                            }
                        },
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
private fun DeleteReveal(progress: Float) {
    val isArmed = progress >= 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // alpha fades in as you swipe — fully opaque when armed
                color = MaterialTheme.colorScheme.errorContainer.copy(
                    alpha = (0.4f + progress * 0.6f).coerceIn(0f, 1f)
                ),
                shape = MaterialTheme.shapes.medium,
            ),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .padding(end = 20.dp)
                .scale(0.85f + progress * 0.3f),   // grows 0.85x → 1.15x as you drag
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = "Delete",
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
            )
        }
    }
}