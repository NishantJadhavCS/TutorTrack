package com.example.tutionprogess.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tutionprogess.data.PreferencesRepository
import com.example.tutionprogess.data.TuitionRepository
import com.example.tutionprogess.domain.CalendarEvent
import com.example.tutionprogess.domain.CalendarEvents
import com.example.tutionprogess.domain.LessonCounts
import com.example.tutionprogess.domain.StudentProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class HomeUiState(
    val today: LocalDate,
    val greeting: String,
    val teacherName: String,
    val students: List<StudentProgress>,
    /** Every taught/learned mark across all students (live from Room). */
    val events: List<CalendarEvent>,
) {
    /** Lesson counts across all students; optional lessons are already excluded. */
    val totals: LessonCounts = students.fold(LessonCounts.Zero) { acc, s -> acc + s.totals }
}

class HomeViewModel(
    private val repository: TuitionRepository,
    preferences: PreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState?> =
        combine(repository.observeStudentsWithSubjects(), preferences.profile) { students, profile ->
            val today = LocalDate.now()
            val progress = students.map { StudentProgress.of(it, today) }
            HomeUiState(
                today = today,
                greeting = greetingFor(LocalTime.now()),
                teacherName = profile.name,
                students = progress,
                events = CalendarEvents.build(progress),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Moves a taught/learned mark to another day (from the calendar). */
    fun changeEventDate(event: CalendarEvent, date: LocalDate) {
        viewModelScope.launch { repository.setLessonDate(event.lessonId, event.kind, date) }
    }
}

fun greetingFor(time: LocalTime): String = when (time.hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
