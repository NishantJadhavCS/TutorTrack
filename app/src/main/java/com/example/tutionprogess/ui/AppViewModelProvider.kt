package com.example.tutionprogess.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tutionprogess.TuitionApp
import com.example.tutionprogess.ui.home.HomeViewModel
import com.example.tutionprogess.ui.profile.ProfileViewModel
import com.example.tutionprogess.ui.settings.SettingsViewModel
import com.example.tutionprogess.ui.student.StudentViewModel
import com.example.tutionprogess.ui.subject.SubjectViewModel

/** Creates every ViewModel with its dependencies from [TuitionApp]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { HomeViewModel(app().repository, app().preferences) }
        initializer { StudentViewModel(createSavedStateHandle(), app().repository) }
        initializer { SubjectViewModel(createSavedStateHandle(), app().repository) }
        initializer { ProfileViewModel(app().preferences) }
        initializer { SettingsViewModel(app().repository, app().preferences, app().backupManager) }
    }
}

private fun CreationExtras.app(): TuitionApp = this[APPLICATION_KEY] as TuitionApp
