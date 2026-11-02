package com.example.tutionprogess.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class BackupFormatException(message: String) : Exception(message)

/** A parsed, validated backup file, ready to replace the current data. */
data class DecodedBackup(
    val data: BackupData,
    val profile: TeacherProfile?,
    val exportedAt: String?,
)

/**
 * Converts all app data to and from a JSON backup file.
 * The file format is kept separate from the Room entities so the database can change
 * without breaking old backups.
 */
object BackupCodec {
    private const val APP_ID = "tuition-progress"
    private const val FORMAT_VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(data: BackupData, profile: TeacherProfile, exportedAt: LocalDateTime): String =
        json.encodeToString(
            BackupFile.serializer(),
            BackupFile(
                app = APP_ID,
                formatVersion = FORMAT_VERSION,
                exportedAt = exportedAt.withNano(0).toString(),
                profile = ProfileDto(profile.name, profile.className, profile.phone),
                students = data.students.map {
                    StudentDto(
                        id = it.id,
                        fullName = it.fullName,
                        standard = it.standard,
                        trackingStartDate = it.trackingStartDate.toString(),
                        semesterStartDate = it.semesterStartDate?.toString(),
                        semesterIsMonthOnly = it.semesterIsMonthOnly,
                        createdAt = it.createdAt,
                    )
                },
                subjects = data.subjects.map { SubjectDto(it.id, it.studentId, it.name, it.sortOrder, it.isStarred) },
                lessons = data.lessons.map {
                    LessonDto(
                        id = it.id,
                        subjectId = it.subjectId,
                        number = it.number,
                        title = it.title,
                        isOptional = it.isOptional,
                        taughtOn = it.taughtOn?.toString(),
                        learnedOn = it.learnedOn?.toString(),
                    )
                },
            ),
        )

    /** Parses and validates [text]; throws [BackupFormatException] with a readable message. */
    fun decode(text: String): DecodedBackup {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (_: IllegalArgumentException) { // includes SerializationException
            throw BackupFormatException("This file isn't a TutorTrack backup.")
        }
        if (file.app != APP_ID) throw BackupFormatException("This file isn't a TutorTrack backup.")
        if (file.formatVersion > FORMAT_VERSION) {
            throw BackupFormatException("This backup was made by a newer version of the app.")
        }

        val students = file.students.map {
            Student(
                id = it.id,
                fullName = it.fullName,
                standard = it.standard,
                trackingStartDate = parseDate(it.trackingStartDate),
                semesterStartDate = it.semesterStartDate?.let(::parseDate),
                semesterIsMonthOnly = it.semesterIsMonthOnly,
                createdAt = it.createdAt,
            )
        }
        val subjects = file.subjects.map { Subject(it.id, it.studentId, it.name, it.sortOrder, it.isStarred) }
        val lessons = file.lessons.map {
            Lesson(
                id = it.id,
                subjectId = it.subjectId,
                number = it.number,
                title = it.title,
                isOptional = it.isOptional,
                taughtOn = it.taughtOn?.let(::parseDate),
                learnedOn = it.learnedOn?.let(::parseDate),
            )
        }

        requireUniqueIds(students.map { it.id }, "student")
        requireUniqueIds(subjects.map { it.id }, "subject")
        requireUniqueIds(lessons.map { it.id }, "lesson")
        val studentIds = students.mapTo(HashSet()) { it.id }
        val subjectIds = subjects.mapTo(HashSet()) { it.id }
        if (subjects.any { it.studentId !in studentIds }) throw BackupFormatException("The backup is incomplete: a subject has no student.")
        if (lessons.any { it.subjectId !in subjectIds }) throw BackupFormatException("The backup is incomplete: a lesson has no subject.")

        return DecodedBackup(
            data = BackupData(students, subjects, lessons),
            profile = file.profile?.let { TeacherProfile(it.name, it.className, it.phone) },
            exportedAt = file.exportedAt,
        )
    }

    private fun parseDate(value: String): LocalDate = try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        throw BackupFormatException("The backup contains an invalid date: $value")
    }

    private fun requireUniqueIds(ids: List<Long>, kind: String) {
        if (ids.any { it <= 0 } || ids.toSet().size != ids.size) {
            throw BackupFormatException("The backup has invalid $kind ids.")
        }
    }
}

@Serializable
private data class BackupFile(
    val app: String = "",
    val formatVersion: Int = 0,
    val exportedAt: String? = null,
    val profile: ProfileDto? = null,
    val students: List<StudentDto> = emptyList(),
    val subjects: List<SubjectDto> = emptyList(),
    val lessons: List<LessonDto> = emptyList(),
)

@Serializable
private data class ProfileDto(
    val name: String = "",
    val className: String = "",
    val phone: String = "",
)

@Serializable
private data class StudentDto(
    val id: Long,
    val fullName: String,
    val standard: String = "",
    val trackingStartDate: String,
    val semesterStartDate: String? = null,
    val semesterIsMonthOnly: Boolean = false,
    val createdAt: Long = 0,
)

@Serializable
private data class SubjectDto(
    val id: Long,
    val studentId: Long,
    val name: String,
    val sortOrder: Int = 0,
    val isStarred: Boolean = false,
)

@Serializable
private data class LessonDto(
    val id: Long,
    val subjectId: Long,
    val number: Int,
    val title: String? = null,
    val isOptional: Boolean = false,
    val taughtOn: String? = null,
    val learnedOn: String? = null,
)
