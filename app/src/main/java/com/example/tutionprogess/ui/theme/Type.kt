package com.example.tutionprogess.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutionprogess.R

/**
 * Manrope (Google Fonts, OFL), bundled as static files per weight. (The single variable-font
 * file rendered every weight as its thinnest default on device, so static instances are used.)
 */
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

private val base = Typography()

private fun TextStyle.manrope(weight: FontWeight? = null) =
    copy(fontFamily = Manrope, fontWeight = weight ?: fontWeight)

// Manrope across the whole scale; slightly heavier headings for a clear hierarchy.
val Typography = Typography(
    displayLarge = base.displayLarge.manrope(FontWeight.Bold),
    displayMedium = base.displayMedium.manrope(FontWeight.Bold),
    displaySmall = base.displaySmall.manrope(FontWeight.Bold),
    headlineLarge = base.headlineLarge.manrope(FontWeight.Bold),
    headlineMedium = base.headlineMedium.manrope(FontWeight.Bold),
    headlineSmall = base.headlineSmall.manrope(FontWeight.Bold),
    titleLarge = base.titleLarge.manrope(FontWeight.Bold),
    titleMedium = base.titleMedium.manrope(FontWeight.SemiBold),
    titleSmall = base.titleSmall.manrope(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.manrope(),
    bodyMedium = base.bodyMedium.manrope(),
    bodySmall = base.bodySmall.manrope(),
    labelLarge = base.labelLarge.manrope(FontWeight.SemiBold),
    labelMedium = base.labelMedium.manrope(FontWeight.Medium).copy(letterSpacing = 0.2.sp),
    labelSmall = base.labelSmall.manrope(FontWeight.Medium),
)

/** Style for headline statistics (big counts and percentages). */
val StatNumber: TextStyle = base.headlineMedium.manrope(FontWeight.ExtraBold).copy(letterSpacing = (-0.5).sp)

val Shapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
