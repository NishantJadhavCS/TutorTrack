package com.example.tutionprogess.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
private val LocalCalendarColors = staticCompositionLocalOf { LightCalendarColors }

@Composable
fun TutionProgessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
        LocalCalendarColors provides if (darkTheme) DarkCalendarColors else LightCalendarColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}

/** Access with `MaterialTheme.statusColors`. */
val MaterialTheme.statusColors: StatusColors
    @Composable @ReadOnlyComposable get() = LocalStatusColors.current

/** Access with `MaterialTheme.calendarColors`. */
val MaterialTheme.calendarColors: CalendarColors
    @Composable @ReadOnlyComposable get() = LocalCalendarColors.current
