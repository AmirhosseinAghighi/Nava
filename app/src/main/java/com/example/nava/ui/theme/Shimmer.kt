package com.example.nava.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "navaShimmer")
    val progress = transition.animateFloat(
        initialValue = SHIMMER_START,
        targetValue = SHIMMER_END,
        animationSpec = infiniteRepeatable(
            animation = tween(NavaMotion.Shimmer, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "navaShimmerProgress",
    ).value
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = SHIMMER_HIGHLIGHT_ALPHA)
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(progress - SHIMMER_WIDTH, progress - SHIMMER_WIDTH),
        end = Offset(progress, progress),
    )
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(rememberShimmerBrush()))
}

private const val SHIMMER_START = -800f
private const val SHIMMER_END = 1_800f
private const val SHIMMER_WIDTH = 420f
private const val SHIMMER_HIGHLIGHT_ALPHA = .12f
