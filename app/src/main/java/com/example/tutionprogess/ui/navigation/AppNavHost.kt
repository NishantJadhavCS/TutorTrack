package com.example.tutionprogess.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tutionprogess.ui.home.HomeScreen
import com.example.tutionprogess.ui.profile.ProfileScreen
import com.example.tutionprogess.ui.settings.SettingsScreen
import com.example.tutionprogess.ui.student.StudentScreen
import com.example.tutionprogess.ui.subject.SubjectScreen
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data class StudentRoute(val studentId: Long)
@Serializable data class SubjectRoute(val subjectId: Long)
@Serializable data object ProfileRoute
@Serializable data object SettingsRoute

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenStudent = { navController.navigate(StudentRoute(it)) },
                onOpenProfile = { navController.navigate(ProfileRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenSubject = { navController.navigate(SubjectRoute(it)) },
            )
        }
        composable<StudentRoute> {
            StudentScreen(
                onBack = { navController.popBackStack() },
                onOpenSubject = { navController.navigate(SubjectRoute(it)) },
            )
        }
        composable<SubjectRoute> {
            SubjectScreen(onBack = { navController.popBackStack() })
        }
        composable<ProfileRoute> {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
