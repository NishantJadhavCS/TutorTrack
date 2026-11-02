package com.example.tutionprogess.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp

/** Card surface with a faint wash of the brand colour instead of plain grey/white. */
@Composable
@ReadOnlyComposable
fun tintedCardColor(base: Color = MaterialTheme.colorScheme.surfaceContainerLow): Color =
    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f).compositeOver(base)

/**
 * Lays out card content next to a thin accent bar on the leading edge.
 * Use as the card's only child; the card's shape clips the bar's corners.
 */
@Composable
fun AccentBarRow(accent: Color, content: @Composable RowScope.() -> Unit) {
    val color by animateColorAsState(accent, label = "accent")
    Row(Modifier.height(IntrinsicSize.Min)) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(color))
        content()
    }
}
