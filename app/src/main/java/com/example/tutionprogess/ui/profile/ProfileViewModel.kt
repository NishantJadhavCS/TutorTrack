package com.example.tutionprogess.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tutionprogess.data.PreferencesRepository
import com.example.tutionprogess.data.TeacherProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Holds the editable profile form; it lives in the ViewModel so edits survive rotation. */
class ProfileViewModel(private val preferences: PreferencesRepository) : ViewModel() {
    var name by mutableStateOf("")
    var className by mutableStateOf("")
    var phone by mutableStateOf("")
    var loaded by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val profile = preferences.profile.first()
            name = profile.name
            className = profile.className
            phone = profile.phone
            loaded = true
        }
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            preferences.saveProfile(TeacherProfile(name, className, phone))
            onSaved()
        }
    }
}
