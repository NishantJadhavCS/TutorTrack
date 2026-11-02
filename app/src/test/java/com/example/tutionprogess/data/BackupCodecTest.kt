package com.example.tutionprogess.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BackupCodecTest {
    private val day = LocalDate.of(2026, 11, 2)
    private val data = BackupData(
        students = listOf(
            Student(1, "Atharva", "8th", day, LocalDate.of(2027, 3, 1), semesterIsMonthOnly = true, createdAt = 10),
            Student(2, "Anwisha", "", day, null, createdAt = 11),
        ),
        subjects = listOf(
            Subject(5, 1, "English", 0, isStarred = true),
            Subject(6, 2, "Maths", 0),
        ),
        lessons = listOf(
            Lesson(9, 5, 1, "Poem", isOptional = false, taughtOn = day, learnedOn = day.plusDays(1)),
            Lesson(10, 5, 2, null, isOptional = true),
            Lesson(11, 6, 1),
        ),
    )
    private val profile = TeacherProfile("Nishant", "Bright Minds", "12345")

    @Test
    fun roundTripKeepsEverything() {
        val text = BackupCodec.encode(data, profile, LocalDateTime.of(2026, 11, 2, 18, 30))
        val decoded = BackupCodec.decode(text)
        assertEquals(data, decoded.data)
        assertEquals(profile, decoded.profile)
        assertEquals("2026-11-02T18:30", decoded.exportedAt)
    }

    @Test
    fun rejectsNonBackupFiles() {
        assertFormatError("not json at all")
        assertFormatError("""{"hello": "world"}""")
        assertFormatError("[]")
    }

    @Test
    fun rejectsNewerFormat() {
        val text = BackupCodec.encode(data, profile, LocalDateTime.now()).replace("\"formatVersion\": 1", "\"formatVersion\": 99")
        assertFormatError(text, "newer version")
    }

    @Test
    fun rejectsOrphans() {
        val orphanLesson = data.copy(lessons = data.lessons + Lesson(12, 999, 1))
        assertFormatError(BackupCodec.encode(orphanLesson, profile, LocalDateTime.now()), "lesson has no subject")

        val orphanSubject = data.copy(subjects = data.subjects + Subject(7, 999, "X", 0))
        assertFormatError(BackupCodec.encode(orphanSubject, profile, LocalDateTime.now()), "subject has no student")
    }

    @Test
    fun rejectsBadDatesAndDuplicateIds() {
        val text = BackupCodec.encode(data, profile, LocalDateTime.now()).replace("2026-11-02", "2026-13-45")
        assertFormatError(text, "invalid date")

        val duplicate = data.copy(students = data.students + data.students.first().copy(fullName = "Copy"))
        assertFormatError(BackupCodec.encode(duplicate, profile, LocalDateTime.now()), "student ids")
    }

    private fun assertFormatError(text: String, contains: String? = null) {
        try {
            BackupCodec.decode(text)
            throw AssertionError("Expected BackupFormatException")
        } catch (e: BackupFormatException) {
            if (contains != null) assertTrue(e.message, e.message!!.contains(contains))
        }
    }
}
