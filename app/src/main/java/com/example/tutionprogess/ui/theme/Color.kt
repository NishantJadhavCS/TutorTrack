package com.example.tutionprogess.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Deep indigo brand palette (hand-tuned, not dynamic colour).

val LightColors = lightColorScheme(
    primary = Color(0xFF3D4DB0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDEE0FF),
    onPrimaryContainer = Color(0xFF00105C),
    inversePrimary = Color(0xFFBAC3FF),
    secondary = Color(0xFF5B5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E1F9),
    onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF00677F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB7EAFF),
    onTertiaryContainer = Color(0xFF001F28),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2EFF7),
    surfaceDim = Color(0xFFDBD9E0),
    surfaceBright = Color(0xFFFBF8FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFEDF4),
    surfaceContainerHigh = Color(0xFFEAE7EF),
    surfaceContainerHighest = Color(0xFFE4E1E9),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF08218A),
    primaryContainer = Color(0xFF253596),
    onPrimaryContainer = Color(0xFFDEE0FF),
    inversePrimary = Color(0xFF3D4DB0),
    secondary = Color(0xFFC4C5DD),
    onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF434559),
    onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFF5CD5F9),
    onTertiary = Color(0xFF003543),
    tertiaryContainer = Color(0xFF004E60),
    onTertiaryContainer = Color(0xFFB7EAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF91909A),
    outlineVariant = Color(0xFF46464F),
    inverseSurface = Color(0xFFE4E1E9),
    inverseOnSurface = Color(0xFF303036),
    surfaceDim = Color(0xFF121318),
    surfaceBright = Color(0xFF39383F),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF29292F),
    surfaceContainerHighest = Color(0xFF34343A),
)

/** Progress status colours, tuned separately for light and dark backgrounds. */
data class StatusColors(
    val green: Color,
    val amber: Color,
    val red: Color,
    val grey: Color,
)

val LightStatusColors = StatusColors(
    green = Color(0xFF1E7B3C),
    amber = Color(0xFF9A5B00),
    red = Color(0xFFC0262D),
    grey = Color(0xFF6B6B76),
)

val DarkStatusColors = StatusColors(
    green = Color(0xFF6EDB8F),
    amber = Color(0xFFFFC25C),
    red = Color(0xFFFF8A84),
    grey = Color(0xFFA3A2AD),
)

/**
 * Calendar event colours: blue for Taught, purple for Learned. Deliberately distinct from the
 * red/amber/green status colours so the two meanings never mix.
 */
data class CalendarColors(
    val taught: Color,
    val taughtContainer: Color,
    val onTaughtContainer: Color,
    val learned: Color,
    val learnedContainer: Color,
    val onLearnedContainer: Color,
)

val LightCalendarColors = CalendarColors(
    taught = Color(0xFF1F5FD1),
    taughtContainer = Color(0xFFD9E5FF),
    onTaughtContainer = Color(0xFF0B2A66),
    learned = Color(0xFF8B3FC6),
    learnedContainer = Color(0xFFF1DDFF),
    onLearnedContainer = Color(0xFF3A0F5C),
)

val DarkCalendarColors = CalendarColors(
    taught = Color(0xFF8EB2FF),
    taughtContainer = Color(0xFF1C3768),
    onTaughtContainer = Color(0xFFDCE6FF),
    learned = Color(0xFFD5A8FF),
    learnedContainer = Color(0xFF4A2470),
    onLearnedContainer = Color(0xFFF3E3FF),
)
