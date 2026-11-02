package com.example.tutionprogess

import android.app.Application
import com.example.tutionprogess.data.AppDatabase
import com.example.tutionprogess.data.BackupManager
import com.example.tutionprogess.data.PreferencesRepository
import com.example.tutionprogess.data.TuitionRepository

/** Holds app-wide singletons (simple manual dependency injection). */
class TuitionApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val repository: TuitionRepository by lazy { TuitionRepository(database) }
    val preferences: PreferencesRepository by lazy { PreferencesRepository(this) }
    val backupManager: BackupManager by lazy { BackupManager(contentResolver, repository, preferences) }
}
