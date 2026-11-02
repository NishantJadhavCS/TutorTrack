package com.example.tutionprogess.domain

import com.example.tutionprogess.data.Lesson
import com.example.tutionprogess.data.Student
import com.example.tutionprogess.data.StudentWithSubjects
import com.example.tutionprogess.data.Subject
import com.example.tutionprogess.data.SubjectWithLessons
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CalendarEventsTest {
    private val start = LocalDate.of(2026, 10, 1)
    private val day1 = LocalDate.of(2026, 10, 12)
    private val day2 = LocalDate.of(2026, 10, 13)

    @Test
    fun subjectShortLabels() {
        assertEquals("Eng", CalendarEvents.subjectShortLabel("English"))
        assertEquals("Mat", CalendarEvents.subjectShortLabel("Maths"))
        assertEquals("Pol", CalendarEvents.subjectShortLabel("Political Science"))
        assertEquals("His", CalendarEvents.subjectShortLabel("  history"))
        assertEquals("Ar", CalendarEvents.subjectShortLabel("Ar"))
    }

    @Test
    fun initialsFromFirstAndLastWord() {
        assertEquals("AD", CalendarEvents.initials("Atharva Deshmukh"))
        assertEquals("AJ", CalendarEvents.initials("anwisha kumar jadhav"))
        assertEquals("At", CalendarEvents.initials("Atharva"))
    }

    @Test
    fun clashingInitialsFallBackToFirstTwoLettersOfFirstName() {
        val students = listOf(
            Student(id = 1, fullName = "Atharva Dodake", trackingStartDate = start),
            Student(id = 2, fullName = "Anwisha Desai", trackingStartDate = start),
            Student(id = 3, fullName = "Riya Shah", trackingStartDate = start),
        )
        assertEquals(mapOf(1L to "At", 2L to "An", 3L to "RS"), CalendarEvents.studentInitials(students))
    }

    @Test
    fun buildsTaughtAndLearnedEventsInStudentSubjectLessonOrder() {
        fun subject(id: Long, studentId: Long, name: String, order: Int, vararg lessons: Lesson) =
            SubjectWithLessons(Subject(id, studentId, name, order), lessons.toList())

        val atharva = StudentWithSubjects(
            Student(id = 1, fullName = "Atharva Dodake", trackingStartDate = start),
            listOf(
                // Out of order on purpose: Maths has sortOrder 0, English 1.
                subject(11, 1, "English", 1, Lesson(101, 11, 2, taughtOn = day1), Lesson(100, 11, 1, taughtOn = day1, learnedOn = day2)),
                subject(10, 1, "Maths", 0, Lesson(102, 10, 3, "Fractions", isOptional = true, taughtOn = day1)),
            ),
        )
        val anwisha = StudentWithSubjects(
            Student(id = 2, fullName = "Anwisha Jadhav", trackingStartDate = start),
            listOf(subject(20, 2, "Science", 0, Lesson(200, 20, 1, taughtOn = day1), Lesson(201, 20, 2))),
        )
        val today = LocalDate.of(2026, 11, 2)
        val events = CalendarEvents.build(listOf(atharva, anwisha).map { StudentProgress.of(it, today) })

        assertEquals(
            listOf(
                "AD Mat 3 TAUGHT $day1",
                "AD Eng 1 TAUGHT $day1",
                "AD Eng 1 LEARNED $day2",
                "AD Eng 2 TAUGHT $day1",
                "AJ Sci 1 TAUGHT $day1",
            ),
            events.map { "${it.studentInitials} ${it.subjectShort} ${it.lessonNumber} ${it.kind} ${it.date}" },
        )
        // Optional lessons are included (shown muted), with titles in the full label.
        val maths = events.first()
        assertEquals(true, maths.isOptional)
        assertEquals("Lesson 3 - Fractions", maths.lessonLabel)
        assertEquals("Lesson 1", events[1].lessonLabel)
        // Grouping by day keeps the student/subject/lesson order.
        assertEquals(listOf(102L, 100L, 101L, 200L), events.groupBy { it.date }.getValue(day1).map { it.lessonId })
    }
}
