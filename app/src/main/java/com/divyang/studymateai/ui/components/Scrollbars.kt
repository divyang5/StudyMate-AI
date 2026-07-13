package com.divyang.studymateai.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ThumbThickness = 4.dp
private val ThumbDraggingThickness = 7.dp
private val ThumbMinHeight = 36.dp

// Width of the invisible strip along the right edge where a drag grabs the
// thumb (the thumb itself is too thin to hit reliably).
private val GrabWidth = 40.dp
private val GrabSlopY = 16.dp

/** Thumb geometry plus the scroll mapping needed to scrub. */
private class ThumbMetrics(
    val top: Float,
    val height: Float,
    val scrolled: Float,
    val maxScroll: Float
)

private fun scrollStateMetrics(state: ScrollState, viewportHeight: Float, minThumbPx: Float): ThumbMetrics? {
    // maxValue is Int.MAX_VALUE until the first measure pass
    if (state.maxValue <= 0 || state.maxValue == Int.MAX_VALUE) return null
    val total = viewportHeight + state.maxValue
    val height = (viewportHeight / total * viewportHeight).coerceAtLeast(minThumbPx)
    val fraction = state.value.toFloat() / state.maxValue
    return ThumbMetrics(
        top = (viewportHeight - height) * fraction,
        height = height,
        scrolled = state.value.toFloat(),
        maxScroll = state.maxValue.toFloat()
    )
}

/**
 * Thumb size/position estimated from the average visible item stride — exact
 * for uniform lists, close enough for mixed content.
 */
private fun lazyListMetrics(state: LazyListState, viewportHeight: Float, minThumbPx: Float): ThumbMetrics? {
    val info = state.layoutInfo
    val visible = info.visibleItemsInfo
    if (visible.isEmpty()) return null
    val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
    val first = visible.first()
    val stride =
        if (visible.size > 1) (visible.last().offset - first.offset).toFloat() / (visible.size - 1)
        else first.size.toFloat()
    if (stride <= 0f) return null
    val totalHeight = stride * info.totalItemsCount
    if (totalHeight <= viewport) return null
    val height = (viewport / totalHeight * viewportHeight).coerceAtLeast(minThumbPx)
    val maxScroll = totalHeight - viewport
    val scrolled = (first.index * stride - first.offset).coerceIn(0f, maxScroll)
    return ThumbMetrics(
        top = (viewportHeight - height) * (scrolled / maxScroll),
        height = height,
        scrolled = scrolled,
        maxScroll = maxScroll
    )
}

@Composable
private fun scrollbarAlpha(visible: Boolean): Float {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (visible) tween(durationMillis = 150)
        else tween(durationMillis = 500, delayMillis = 1000),
        label = "scrollbarAlpha"
    )
    return alpha
}

/**
 * Fading, draggable scrollbar for a Column/Row with `verticalScroll`. Grab the
 * thumb (a generous strip along the right edge) and drag to jump anywhere.
 */
@Composable
fun Modifier.verticalScrollbar(
    state: ScrollState,
    color: Color = AppColors.Purple.copy(alpha = 0.5f)
): Modifier {
    var isDragging by remember { mutableStateOf(false) }
    val alpha = scrollbarAlpha(state.isScrollInProgress || isDragging)
    return this
        .pointerInput(state) {
            val minThumb = ThumbMinHeight.toPx()
            val grabWidth = GrabWidth.toPx()
            val slopY = GrabSlopY.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val viewportHeight = size.height.toFloat()
                val m = scrollStateMetrics(state, viewportHeight, minThumb) ?: return@awaitEachGesture
                val onThumb = down.position.x >= size.width - grabWidth &&
                        down.position.y in (m.top - slopY)..(m.top + m.height + slopY)
                if (!onThumb) return@awaitEachGesture
                isDragging = true
                try {
                    verticalDrag(down.id) { change ->
                        change.consume()
                        val cur = scrollStateMetrics(state, viewportHeight, minThumb) ?: return@verticalDrag
                        val track = viewportHeight - cur.height
                        if (track <= 0f) return@verticalDrag
                        val fraction = ((change.position.y - cur.height / 2) / track).coerceIn(0f, 1f)
                        state.dispatchRawDelta(fraction * cur.maxScroll - cur.scrolled)
                    }
                } finally {
                    isDragging = false
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (alpha <= 0f) return@drawWithContent
            val m = scrollStateMetrics(state, size.height, ThumbMinHeight.toPx()) ?: return@drawWithContent
            drawThumb(m, color, alpha, isDragging)
        }
}

/**
 * Fading, draggable scrollbar for a LazyColumn. Grab the thumb (a generous
 * strip along the right edge) and drag to jump anywhere in the list.
 */
@Composable
fun Modifier.verticalScrollbar(
    state: LazyListState,
    color: Color = AppColors.Purple.copy(alpha = 0.5f)
): Modifier {
    var isDragging by remember { mutableStateOf(false) }
    val alpha = scrollbarAlpha(state.isScrollInProgress || isDragging)
    return this
        .pointerInput(state) {
            val minThumb = ThumbMinHeight.toPx()
            val grabWidth = GrabWidth.toPx()
            val slopY = GrabSlopY.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val viewportHeight = size.height.toFloat()
                val m = lazyListMetrics(state, viewportHeight, minThumb) ?: return@awaitEachGesture
                val onThumb = down.position.x >= size.width - grabWidth &&
                        down.position.y in (m.top - slopY)..(m.top + m.height + slopY)
                if (!onThumb) return@awaitEachGesture
                isDragging = true
                try {
                    verticalDrag(down.id) { change ->
                        change.consume()
                        val cur = lazyListMetrics(state, viewportHeight, minThumb) ?: return@verticalDrag
                        val track = viewportHeight - cur.height
                        if (track <= 0f) return@verticalDrag
                        val fraction = ((change.position.y - cur.height / 2) / track).coerceIn(0f, 1f)
                        state.dispatchRawDelta(fraction * cur.maxScroll - cur.scrolled)
                    }
                } finally {
                    isDragging = false
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (alpha <= 0f) return@drawWithContent
            val m = lazyListMetrics(state, size.height, ThumbMinHeight.toPx()) ?: return@drawWithContent
            drawThumb(m, color, alpha, isDragging)
        }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawThumb(
    m: ThumbMetrics,
    color: Color,
    alpha: Float,
    isDragging: Boolean
) {
    val thickness = (if (isDragging) ThumbDraggingThickness else ThumbThickness).toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width - thickness, m.top),
        size = Size(thickness, m.height),
        cornerRadius = CornerRadius(thickness / 2),
        alpha = alpha
    )
}
