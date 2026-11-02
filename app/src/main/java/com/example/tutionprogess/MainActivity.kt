package com.example.tutionprogess

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tutionprogess.data.ThemeMode
import com.example.tutionprogess.ui.navigation.AppNavHost
import com.example.tutionprogess.ui.theme.TutionProgessTheme

class MainActivity : ComponentActivity() {
    /** Set once the saved theme has been read; the splash screen stays up until then. */
    private var themeLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !themeLoaded }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = (application as TuitionApp).preferences
        setContent {
            // Null until the saved theme is read (a few ms); the splash covers that gap.
            val themeMode by preferences.themeMode.collectAsStateWithLifecycle(initialValue = null)
            val mode = themeMode ?: return@setContent
            SideEffect { themeLoaded = true }
            val darkTheme = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Keep status/navigation bar icons readable when the app theme differs from the system.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }
            TutionProgessTheme(darkTheme = darkTheme) {
                AppNavHost()
            }
        }
    }
}
