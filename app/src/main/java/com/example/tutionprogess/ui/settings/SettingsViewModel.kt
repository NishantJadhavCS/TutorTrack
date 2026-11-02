package com.example.tutionprogess.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tutionprogess.data.BackupFormatException
import com.example.tutionprogess.data.BackupManager
import com.example.tutionprogess.data.DecodedBackup
import com.example.tutionprogess.data.PreferencesRepository
import com.example.tutionprogess.data.Student
import com.example.tutionprogess.data.StudentWithSubjects
import com.example.tutionprogess.data.ThemeMode
import com.example.tutionprogess.data.TuitionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: TuitionRepository,
    private val preferences: PreferencesRepository,
    private val backup: BackupManager,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode?> =
        preferences.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val students: StateFlow<List<StudentWithSubjects>?> =
        repository.observeStudentsWithSubjects().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** A backup that has been read and validated, waiting for the user to confirm the import. */
    private val _pendingImport = MutableStateFlow<DecodedBackup?>(null)
    val pendingImport: StateFlow<DecodedBackup?> = _pendingImport.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun saveStudent(student: Student) {
        viewModelScope.launch {
            if (student.id == 0L) repository.addStudent(student) else repository.updateStudent(student)
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            _messages.send("${student.fullName} deleted")
        }
    }

    fun export(uri: Uri) = runIo(failure = "Couldn't save the backup") {
        backup.export(uri)
        _messages.send("Backup saved")
    }

    fun readImport(uri: Uri) = runIo(failure = "Couldn't read that file") {
        _pendingImport.value = backup.read(uri)
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    fun confirmImport() {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        runIo(failure = "Couldn't restore the backup") {
            backup.restore(pending)
            _messages.send("Backup restored")
        }
    }

    private fun runIo(failure: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupFormatException) {
                _messages.send(e.message ?: failure)
            } catch (_: Exception) {
                _messages.send(failure)
            } finally {
                _busy.value = false
            }
        }
    }
}
