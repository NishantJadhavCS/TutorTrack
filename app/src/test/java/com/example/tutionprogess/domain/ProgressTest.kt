package com.example.tutionprogess.domain

import com.example.tutionprogess.data.Lesson
import com.example.tutionprogess.data.Student
import com.example.tutionprogess.data.StudentWithSubjects
import com.example.tutionprogess.data.Subject
import com.example.tutionprogess.data.SubjectWithLessons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class TeachingCalendarTest {
    private val monday = LocalDate.of(2026, 11, 2)
    private val sunday = LocalDate.of(2026, 11, 1)

    @Test
    fun fixturesAreOnExpectedWeekdays() {
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
    }

    @Test
    fun fullWeekHasSixTeachingDays() {
        assertEquals(6, TeachingCalendar.teachingDaysBetween(monday, monday.plusWeeks(1)))
        assertEquals(12, TeachingCalendar.teachingDaysBetween(monday, monday.plusWeeks(2)))
    }

    @Test
    fun sundayIsExcluded() {
        assertEquals(0, TeachingCalendar.teachingDaysBetween(sunday, sunday.plusDays(1)))
        // Monday to Saturday inclusive.
        assertEquals(6, TeachingCalendar.teachingDaysBetween(monday, monday.plusDays(6)))
        // Saturday through Monday (exclusive) contains only Saturday.
        assertEquals(1, TeachingCalendar.teachingDaysBetween(sunday.minusDays(1), monday))
    }

    @Test
    fun emptyOrReversedRangeIsZero() {
        assertEquals(0, TeachingCalendar.teachingDaysBetween(monday, monday))
        assertEquals(0, TeachingCalendar.teachingDaysBetween(monday, monday.minusDays(10)))
    }

    @Test
    fun matchesDayByDayCountForManyRanges() {
        for (startOffset in 0L until 7L) {
            val start = monday.plusDays(startOffset)
            for (length in 0L until 60L) {
                val end = start.plusDays(length)
                val expected = (0 until length).count { start.plusDays(it).dayOfWeek != DayOfWeek.SUNDAY }
                assertEquals("start=$start length=$length", expected, TeachingCalendar.teachingDaysBetween(start, end))
            }
        }
    }
}

class SemesterScheduleTest {
    private val monday = LocalDate.of(2026, 11, 2)

    @Test
    fun daysLeftStartFromTomorrowAndExcludeDeadline() {
        val saturday = LocalDate.of(2026, 10, 31)
        val deadline = LocalDate.of(2026, 11, 9) // Monday
        val schedule = SemesterSchedule.of(saturday, deadline, today = saturday)
        // Sun 1 Nov (excluded) then Mon 2 - Sat 7 Nov; Sun 8 excluded; Mon 9 is the deadline.
        assertEquals(6, schedule.daysLeft)
    }

    @Test
    fun expectedProgressIsElapsedOverTotal() {
        val deadline = monday.plusWeeks(2)
        val schedule = SemesterSchedule.of(monday, deadline, today = monday.plusDays(5)) // Saturday
        assertEquals(12, schedule.totalDays)
        assertEquals(6, schedule.elapsedDays)
        assertEquals(6, schedule.daysLeft)
        assertEquals(0.5, schedule.expectedFraction, 1e-9)
    }

    @Test
    fun expectedProgressIsClamped() {
        val deadline = monday.plusWeeks(2)
        assertEquals(0.0, SemesterSchedule.of(monday, deadline, monday.minusDays(3)).expectedFraction, 1e-9)

        val after = SemesterSchedule.of(monday, deadline, deadline.plusDays(10))
        assertEquals(1.0, after.expectedFraction, 1e-9)
        assertEquals(0, after.daysLeft)
    }

    @Test
    fun deadlineOnOrBeforeTrackingStartMeansFullyExpected() {
        assertEquals(1.0, SemesterSchedule.of(monday, monday, monday).expectedFraction, 1e-9)
    }

    @Test
    fun monthOnlySemesterUsesFirstOfMonth() {
        val student = Student(
            fullName = "A",
            trackingStartDate = monday,
            semesterStartDate = LocalDate.of(2026, 12, 15),
            semesterIsMonthOnly = true,
        )
        assertEquals(LocalDate.of(2026, 12, 1), SemesterSchedule.of(student, monday)!!.deadline)
        assertEquals(LocalDate.of(2026, 12, 15), SemesterSchedule.of(student.copy(semesterIsMonthOnly = false), monday)!!.deadline)
    }

    @Test
    fun noSemesterDateMeansNoSchedule() {
        assertNull(SemesterSchedule.of(Student(fullName = "A", trackingStartDate = monday), monday))
    }
}

class LessonCountsTest {
    private val day = LocalDate.of(2026, 11, 2)

    @Test
    fun optionalLessonsAreExcluded() {
        val lessons = listOf(
            Lesson(subjectId = 1, number = 1, taughtOn = day, learnedOn = day),
            Lesson(subjectId = 1, number = 2, taughtOn = day),
            Lesson(subjectId = 1, number = 3),
            Lesson(subjectId = 1, number = 4),
            Lesson(subjectId = 1, number = 5, isOptional = true, taughtOn = day, learnedOn = day),
        )
        val counts = LessonCounts.of(lessons)
        assertEquals(LessonCounts(total = 4, taught = 2, learned = 1), counts)
        assertEquals(0.5f, counts.taughtFraction, 1e-6f)
        assertEquals(0.25f, counts.learnedFraction, 1e-6f)
        assertEquals(2, counts.leftToTeach)
        assertEquals(3, counts.leftToLearn)
    }

    @Test
    fun emptySubjectHasZeroPercent() {
        val counts = LessonCounts.of(emptyList())
        assertEquals(0f, counts.taughtFraction, 0f)
        assertEquals(0f, counts.learnedFraction, 0f)
    }
}

class StatusTest {
    private fun status(taught: Int, total: Int = 20, expected: Double? = 0.5) =
        ProgressCalculator.status(LessonCounts(total, taught, 0), expected)

    @Test
    fun boundaries() {
        assertEquals(SubjectStatus.ON_TRACK, status(taught = 12)) // 60% vs 50%
        assertEquals(SubjectStatus.ON_TRACK, status(taught = 9)) // 45%: exactly expected - 5
        assertEquals(SubjectStatus.SLIGHTLY_BEHIND, status(taught = 8)) // 40%
        assertEquals(SubjectStatus.SLIGHTLY_BEHIND, status(taught = 7)) // 35%: exactly expected - 15
        assertEquals(SubjectStatus.BEHIND, status(taught = 6)) // 30%
    }

    @Test
    fun doneWhenEverythingTaught() {
        assertEquals(SubjectStatus.DONE, status(taught = 20))
        // Being done doesn't depend on a deadline.
        assertEquals(SubjectStatus.DONE, status(taught = 20, expected = null))
    }

    @Test
    fun noStatusWithoutSemesterOrLessons() {
        assertEquals(SubjectStatus.NONE, status(taught = 5, expected = null))
        assertEquals(SubjectStatus.NONE, status(taught = 0, total = 0))
    }
}

class PaceTextTest {
    @Test
    fun plainWords() {
        assertEquals("about 1 lesson every 3 days", ProgressCalculator.paceText(lessonsLeft = 10, daysLeft = 30))
        assertEquals("about 2 lessons a day", ProgressCalculator.paceText(lessonsLeft = 20, daysLeft = 10))
        assertEquals("about 1 lesson a day", ProgressCalculator.paceText(lessonsLeft = 10, daysLeft = 10))
        assertEquals("about 1 lesson a day", ProgressCalculator.paceText(lessonsLeft = 9, daysLeft = 10))
        assertEquals("All lessons taught", ProgressCalculator.paceText(lessonsLeft = 0, daysLeft = 10))
        assertEquals("No teaching days left", ProgressCalculator.paceText(lessonsLeft = 5, daysLeft = 0))
    }
}

class SubjectOrderTest {
    @Test
    fun followsManualOrderIgnoringStarAndStatus() {
        val monday = LocalDate.of(2026, 11, 2)
        val student = Student(
            id = 1,
            fullName = "A",
            trackingStartDate = monday,
            semesterStartDate = monday.plusWeeks(2),
        )
        // Today is Saturday of week one: expected = 50%.
        val today = monday.plusDays(5)

        fun subject(id: Long, name: String, order: Int, taught: Int, total: Int, starred: Boolean = false) = SubjectWithLessons(
            Subject(id = id, studentId = 1, name = name, sortOrder = order, isStarred = starred),
            (1..total).map { Lesson(subjectId = id, number = it, taughtOn = if (it <= taught) monday else null) },
        )

        val data = StudentWithSubjects(
            student,
            listOf(
                subject(1, "Done", order = 3, taught = 4, total = 4),
                subject(2, "OnTrack", order = 0, taught = 5, total = 10),
                subject(3, "Behind", order = 4, taught = 0, total = 4),
                subject(4, "Starred", order = 2, taught = 2, total = 2, starred = true),
                // Same sortOrder as "OnTrack": the lower id comes first.
                subject(5, "Empty", order = 0, taught = 0, total = 0),
            ),
        )
        val order = StudentProgress.of(data, today).subjects.map { it.subject.name }
        assertEquals(listOf("OnTrack", "Empty", "Starred", "Done", "Behind"), order)
    }
}
