package com.example.tutionprogess.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class TeacherProfile(
    val name: String = "",
    val className: String = "",
    val phone: String = "",
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Settings and the teacher profile, stored with DataStore. */
class PreferencesRepository(context: Context) {
    private val store = context.settingsStore

    private object Keys {
        val TeacherName = stringPreferencesKey("teacher_name")
        val ClassName = stringPreferencesKey("class_name")
        val Phone = stringPreferencesKey("phone")
        val Theme = stringPreferencesKey("theme_mode")
    }

    val profile: Flow<TeacherProfile> = store.data
        .map {
            TeacherProfile(
                name = it[Keys.TeacherName].orEmpty(),
                className = it[Keys.ClassName].orEmpty(),
                phone = it[Keys.Phone].orEmpty(),
            )
        }
        .distinctUntilChanged()

    val themeMode: Flow<ThemeMode> = store.data
        .map { prefs -> ThemeMode.entries.firstOrNull { it.name == prefs[Keys.Theme] } ?: ThemeMode.SYSTEM }
        .distinctUntilChanged()

    suspend fun saveProfile(profile: TeacherProfile) {
        store.edit {
            it[Keys.TeacherName] = profile.name.trim()
            it[Keys.ClassName] = profile.className.trim()
            it[Keys.Phone] = profile.phone.trim()
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.Theme] = mode.name }
    }
}
