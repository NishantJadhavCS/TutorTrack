package com.example.tutionprogess.data

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime

/** Reads and writes backup files chosen with the system file picker (Storage Access Framework). */
class BackupManager(
    private val contentResolver: ContentResolver,
    private val repository: TuitionRepository,
    private val preferences: PreferencesRepository,
) {
    suspend fun export(uri: Uri) {
        val text = BackupCodec.encode(repository.snapshot(), preferences.profile.first(), LocalDateTime.now())
        withContext(Dispatchers.IO) {
            // "wt" truncates, so overwriting an existing file doesn't leave old bytes at the end.
            val stream = contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Couldn't open the file.")
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
    }

    suspend fun read(uri: Uri): DecodedBackup {
        val text = withContext(Dispatchers.IO) {
            val stream = contentResolver.openInputStream(uri) ?: throw IOException("Couldn't open the file.")
            stream.use { it.readBytes().toString(Charsets.UTF_8) }
        }
        return withContext(Dispatchers.Default) { BackupCodec.decode(text) }
    }

    /** Replaces all students, subjects, lessons and the profile with the backup's contents. */
    suspend fun restore(backup: DecodedBackup) {
        repository.replaceAll(backup.data)
        backup.profile?.let { preferences.saveProfile(it) }
    }
}
