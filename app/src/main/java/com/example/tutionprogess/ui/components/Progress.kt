package com.example.tutionprogess.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutionprogess.domain.SubjectStatus
import com.example.tutionprogess.ui.theme.StatNumber
import com.example.tutionprogess.ui.theme.statusColors
import kotlin.math.roundToInt

fun percent(fraction: Float): Int = (fraction * 100).roundToInt()

/**
 * Animates from 0 up to [target] the first time this is shown, then smoothly between later values.
 * "Shown" is saved, so returning to a screen or scrolling an item back into view doesn't replay it.
 */
@Composable
fun animateOnFirstShow(target: Float, durationMillis: Int = 800): Float {
    var shown by rememberSaveable { mutableStateOf(false) }
    val animatable = remember { Animatable(if (shown) target else 0f) }
    LaunchedEffect(target) {
        animatable.animateTo(target, tween(durationMillis, easing = FastOutSlowInEasing))
        shown = true
    }
    return animatable.value
}

/** Whole number that counts up from 0 when first shown. */
@Composable
fun CountUpText(value: Int, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier, suffix: String = "") {
    val shown = animateOnFirstShow(value.toFloat()).roundToInt()
    Text("$shown$suffix", modifier = modifier, style = style)
}

/** Compact circular progress ring with the percentage in the middle and a label underneath. */
@Composable
fun ProgressRing(
    progress: Float,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 6.dp,
) {
    val animated = animateOnFirstShow(progress.coerceIn(0f, 1f))
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$label ${percent(progress)} percent" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = strokeWidth.toPx()
                val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                if (animated > 0f) {
                    drawArc(color, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Text("${percent(animated)}%", style = StatNumber.copy(fontSize = 17.sp, letterSpacing = (-0.3).sp))
        }
        Text(
            label,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Thin rounded progress bar whose fill grows in when first shown. */
@Composable
fun ThinProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val animated = animateOnFirstShow(progress.coerceIn(0f, 1f))
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.fillMaxWidth().height(height)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = radius)
        if (animated > 0f) {
            val width = size.width * animated
            val x = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
            drawRoundRect(color, topLeft = Offset(x, 0f), size = Size(width, size.height), cornerRadius = radius)
        }
    }
}

/** A labelled thin bar: "Taught ........ 60% · 6/10", with the percentage large and bold. */
@Composable
fun LabeledProgressBar(label: String, done: Int, total: Int, color: Color, modifier: Modifier = Modifier) {
    val fraction = if (total == 0) 0f else done.toFloat() / total
    val animated = animateOnFirstShow(fraction)
    Column(modifier.clearAndSetSemantics { contentDescription = "$label ${percent(fraction)} percent, $done of $total" }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                label,
                modifier = Modifier.weight(1f).padding(bottom = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("${percent(animated)}%", style = StatNumber.copy(fontSize = 18.sp, letterSpacing = (-0.3).sp))
            Text(
                " · $done/$total",
                modifier = Modifier.padding(bottom = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ThinProgressBar(fraction, color, Modifier.padding(top = 4.dp))
    }
}

data class StatusStyle(val label: String, val icon: ImageVector, val color: Color)

@Composable
fun SubjectStatus.style(): StatusStyle {
    val colors = MaterialTheme.statusColors
    return when (this) {
        SubjectStatus.DONE -> StatusStyle("Done", Icons.Rounded.CheckCircle, colors.green)
        SubjectStatus.ON_TRACK -> StatusStyle("On track", Icons.Rounded.Check, colors.green)
        SubjectStatus.SLIGHTLY_BEHIND -> StatusStyle("Slightly behind", Icons.Rounded.WarningAmber, colors.amber)
        SubjectStatus.BEHIND -> StatusStyle("Behind", Icons.Rounded.ErrorOutline, colors.red)
        SubjectStatus.NONE -> StatusStyle("No status", Icons.Rounded.Remove, colors.grey)
    }
}

/** Status pill: always icon + label, never colour alone. */
@Composable
fun StatusBadge(status: SubjectStatus, modifier: Modifier = Modifier, text: String? = null) {
    val style = status.style()
    val color by animateColorAsState(style.color, label = "status")
    Surface(modifier = modifier, shape = CircleShape, color = color.copy(alpha = 0.14f), contentColor = color) {
        Row(
            Modifier.padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(style.icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Box(Modifier.width(4.dp))
            Text(text ?: style.label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
