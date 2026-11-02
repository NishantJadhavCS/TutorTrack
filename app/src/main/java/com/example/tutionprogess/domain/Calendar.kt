package com.example.tutionprogess.domain

import com.example.tutionprogess.data.Student
import java.time.LocalDate

enum class LessonEventKind { TAUGHT, LEARNED }

/** One "taught" or "learned" mark on a lesson, placed on the calendar at [date]. */
data class CalendarEvent(
    val date: LocalDate,
    val kind: LessonEventKind,
    val lessonId: Long,
    val lessonNumber: Int,
    val lessonTitle: String?,
    val isOptional: Boolean,
    val subjectId: Long,
    val subjectName: String,
    val subjectShort: String,
    val studentId: Long,
    val studentName: String,
    val studentInitials: String,
    /** Earliest date the event may be moved to. */
    val trackingStart: LocalDate,
) {
    /** "Lesson N - Title", or "Lesson N" when there's no title. */
    val lessonLabel: String
        get() = lessonTitle?.takeIf { it.isNotBlank() }?.let { "Lesson $lessonNumber - $it" } ?: "Lesson $lessonNumber"
}

object CalendarEvents {
    /** First three letters of the subject, e.g. "Political Science" -> "Pol". */
    fun subjectShortLabel(name: String): String = name.trim().take(3).replaceFirstChar { it.uppercaseChar() }

    /** First letter of the first and last word ("Atharva Dodake" -> "AD"); one word -> first two letters. */
    fun initials(fullName: String): String {
        val words = fullName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when (words.size) {
            0 -> "?"
            1 -> twoLetters(words[0])
            else -> "${words.first().first().uppercaseChar()}${words.last().first().uppercaseChar()}"
        }
    }

    /** Initials per student id; students whose initials clash get the first two letters of their first name. */
    fun studentInitials(students: List<Student>): Map<Long, String> {
        val base = students.associate { it.id to initials(it.fullName) }
        val clashing = base.values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        return students.associate { student ->
            val initials = base.getValue(student.id)
            student.id to if (initials in clashing) twoLetters(student.fullName.trim().substringBefore(' ')) else initials
        }
    }

    /**
     * All taught/learned marks, including optional lessons. The list is ordered by student (as given),
     * then subject sort order, then lesson number, so grouping it by date keeps that order per day.
     */
    fun build(students: List<StudentProgress>): List<CalendarEvent> {
        val initials = studentInitials(students.map { it.student })
        return students.flatMap { progress ->
            val student = progress.student
            progress.subjects
                .sortedWith(ProgressCalculator.subjectOrder)
                .flatMap { subject ->
                    val short = subjectShortLabel(subject.subject.name)
                    subject.lessons.sortedBy { it.number }.flatMap { lesson ->
                        listOfNotNull(
                            lesson.taughtOn?.let { it to LessonEventKind.TAUGHT },
                            lesson.learnedOn?.let { it to LessonEventKind.LEARNED },
                        ).map { (date, kind) ->
                            CalendarEvent(
                                date = date,
                                kind = kind,
                                lessonId = lesson.id,
                                lessonNumber = lesson.number,
                                lessonTitle = lesson.title,
                                isOptional = lesson.isOptional,
                                subjectId = subject.subject.id,
                                subjectName = subject.subject.name,
                                subjectShort = short,
                                studentId = student.id,
                                studentName = student.fullName,
                                studentInitials = initials.getValue(student.id),
                                trackingStart = student.trackingStartDate,
                            )
                        }
                    }
                }
        }
    }

    private fun twoLetters(word: String): String =
        word.take(2).lowercase().replaceFirstChar { it.uppercaseChar() }.ifEmpty { "?" }
}
