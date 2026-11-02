package com.example.tutionprogess.domain

import com.example.tutionprogess.data.Lesson
import com.example.tutionprogess.data.Student
import com.example.tutionprogess.data.StudentWithSubjects
import com.example.tutionprogess.data.Subject
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

/** Teaching days are Monday to Saturday; Sundays are excluded. */
object TeachingCalendar {
    fun isTeachingDay(date: LocalDate): Boolean = date.dayOfWeek != DayOfWeek.SUNDAY

    /** Number of teaching days in [start, endExclusive). Returns 0 if the range is empty. */
    fun teachingDaysBetween(start: LocalDate, endExclusive: LocalDate): Int {
        if (!endExclusive.isAfter(start)) return 0
        val totalDays = endExclusive.toEpochDay() - start.toEpochDay()
        val fullWeeks = totalDays / 7
        var count = fullWeeks * 6
        var day = start.plusDays(fullWeeks * 7)
        while (day.isBefore(endExclusive)) {
            if (isTeachingDay(day)) count++
            day = day.plusDays(1)
        }
        return count.toInt()
    }
}

/** Teaching-day schedule between tracking start and the semester deadline, as seen on [today]. */
data class SemesterSchedule(
    val deadline: LocalDate,
    /** Teaching days from tomorrow up to (not including) the deadline. */
    val daysLeft: Int,
    /** Teaching days from tracking start up to (not including) the deadline. */
    val totalDays: Int,
    /** Teaching days from tracking start up to and including today (capped at [totalDays]). */
    val elapsedDays: Int,
) {
    /** Fraction (0..1) of the syllabus that should be taught by now. */
    val expectedFraction: Double
        get() = if (totalDays <= 0) 1.0 else (elapsedDays.toDouble() / totalDays).coerceIn(0.0, 1.0)

    companion object {
        fun of(trackingStart: LocalDate, deadline: LocalDate, today: LocalDate): SemesterSchedule {
            val total = TeachingCalendar.teachingDaysBetween(trackingStart, deadline)
            val elapsed = TeachingCalendar.teachingDaysBetween(trackingStart, today.plusDays(1))
            return SemesterSchedule(
                deadline = deadline,
                daysLeft = TeachingCalendar.teachingDaysBetween(today.plusDays(1), deadline),
                totalDays = total,
                elapsedDays = elapsed.coerceAtMost(total),
            )
        }

        fun of(student: Student, today: LocalDate): SemesterSchedule? =
            student.semesterDeadline?.let { of(student.trackingStartDate, it, today) }
    }
}

/** Lesson counts with optional lessons excluded. */
data class LessonCounts(val total: Int, val taught: Int, val learned: Int) {
    val leftToTeach: Int get() = total - taught
    val leftToLearn: Int get() = total - learned
    val taughtFraction: Float get() = if (total == 0) 0f else taught.toFloat() / total
    val learnedFraction: Float get() = if (total == 0) 0f else learned.toFloat() / total

    operator fun plus(other: LessonCounts) =
        LessonCounts(total + other.total, taught + other.taught, learned + other.learned)

    companion object {
        val Zero = LessonCounts(0, 0, 0)

        fun of(lessons: List<Lesson>): LessonCounts {
            val counted = lessons.filterNot { it.isOptional }
            return LessonCounts(
                total = counted.size,
                taught = counted.count { it.taughtOn != null },
                learned = counted.count { it.learnedOn != null },
            )
        }
    }
}

enum class SubjectStatus { BEHIND, SLIGHTLY_BEHIND, ON_TRACK, DONE, NONE }

object ProgressCalculator {
    private const val EPSILON = 1e-9

    /**
     * Status from taught% compared with expected%:
     * done at 100%, on track within 5 points, slightly behind within 15 points, otherwise behind.
     * [expectedFraction] is null when no semester date is set; then only "done" can be shown.
     */
    fun status(counts: LessonCounts, expectedFraction: Double?): SubjectStatus {
        if (counts.total == 0) return SubjectStatus.NONE
        if (counts.taught >= counts.total) return SubjectStatus.DONE
        if (expectedFraction == null) return SubjectStatus.NONE
        val taughtPct = counts.taught * 100.0 / counts.total
        val expectedPct = expectedFraction * 100.0
        return when {
            taughtPct >= expectedPct - 5 - EPSILON -> SubjectStatus.ON_TRACK
            taughtPct >= expectedPct - 15 - EPSILON -> SubjectStatus.SLIGHTLY_BEHIND
            else -> SubjectStatus.BEHIND
        }
    }

    /** Pace needed in plain words, e.g. "about 1 lesson every 3 days" or "about 2 lessons a day". */
    fun paceText(lessonsLeft: Int, daysLeft: Int): String = when {
        lessonsLeft <= 0 -> "All lessons taught"
        daysLeft <= 0 -> "No teaching days left"
        lessonsLeft >= daysLeft -> {
            val perDay = (lessonsLeft.toDouble() / daysLeft).roundToInt().coerceAtLeast(1)
            if (perDay == 1) "about 1 lesson a day" else "about $perDay lessons a day"
        }
        else -> {
            val every = (daysLeft.toDouble() / lessonsLeft).roundToInt()
            if (every <= 1) "about 1 lesson a day" else "about 1 lesson every $every days"
        }
    }

    /** The user's manual (drag-and-drop) order; id breaks ties for subjects added at the same position. */
    val subjectOrder: Comparator<SubjectProgress> =
        compareBy<SubjectProgress> { it.subject.sortOrder }.thenBy { it.subject.id }
}

data class SubjectProgress(
    val subject: Subject,
    val lessons: List<Lesson>,
    val counts: LessonCounts,
    val status: SubjectStatus,
)

data class StudentProgress(
    val student: Student,
    val schedule: SemesterSchedule?,
    /** Already sorted with [ProgressCalculator.subjectOrder]. */
    val subjects: List<SubjectProgress>,
) {
    val totals: LessonCounts get() = subjects.fold(LessonCounts.Zero) { acc, s -> acc + s.counts }
    val paceText: String? get() = schedule?.let { ProgressCalculator.paceText(totals.leftToTeach, it.daysLeft) }

    companion object {
        fun of(data: StudentWithSubjects, today: LocalDate): StudentProgress {
            val schedule = SemesterSchedule.of(data.student, today)
            val subjects = data.subjects.map { sw ->
                val counts = LessonCounts.of(sw.lessons)
                SubjectProgress(
                    subject = sw.subject,
                    lessons = sw.lessons.sortedBy { it.number },
                    counts = counts,
                    status = ProgressCalculator.status(counts, schedule?.expectedFraction),
                )
            }.sortedWith(ProgressCalculator.subjectOrder)
            return StudentProgress(data.student, schedule, subjects)
        }
    }
}
