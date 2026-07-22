package com.example.nava.ui.theme

import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

object NavaSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
}

object NavaMotion {
    const val Fast = 120
    const val Standard = 220
    const val Slow = 320
}

object NavaDimensions {
    val AuthLogoSize = 104.dp
    val AuthContentMaxWidth = 520.dp
    val AuthButtonMinHeight = 52.dp
    val AuthProgressStrokeWidth = 2.dp
    val PlayerArtworkMaxSize = 300.dp
    val PlayerPrimaryControlSize = 76.dp
    val PlayerSecondaryControlSize = 56.dp
    val PlayerUtilityControlHeight = 52.dp
    val PlayerVisualizerHeight = 48.dp
    val PlayerSwipeThreshold = 88.dp
    val PlayerArtworkBorderWidth = 2.dp
    val HomeTopBarLogoSize = 40.dp
    val HomeFeaturedCardWidth = 286.dp
    val HomeFeaturedCardHeight = 190.dp
    val HomeTrackCardWidth = 172.dp
    val HomeTrackArtworkHeight = 112.dp
    val HomeQuickActionWidth = 154.dp
    val HomeQuickActionHeight = 72.dp
    val MiniPlayerArtworkSize = 48.dp
}

val NavaShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)
