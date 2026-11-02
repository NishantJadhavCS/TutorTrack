package com.example.tutionprogess.ui.student

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.tutionprogess.data.Student
import com.example.tutionprogess.data.TuitionRepository
import com.example.tutionprogess.domain.CalendarEvent
import com.example.tutionprogess.domain.CalendarEvents
import com.example.tutionprogess.domain.StudentProgress
import com.example.tutionprogess.ui.navigation.StudentRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface StudentUiState {
    data object Loading : StudentUiState
    data object Missing : StudentUiState
    data class Loaded(
        val progress: StudentProgress,
        /** This student's taught/learned marks for the calendar view. */
        val events: List<CalendarEvent>,
    ) : StudentUiState
}

class StudentViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TuitionRepository,
) : ViewModel() {
    private val studentId = savedStateHandle.toRoute<StudentRoute>().studentId

    val uiState: StateFlow<StudentUiState> = repository.observeStudent(studentId)
        .map { data ->
            if (data == null) {
                StudentUiState.Missing
            } else {
                val progress = StudentProgress.of(data, LocalDate.now())
                StudentUiState.Loaded(progress, CalendarEvents.build(listOf(progress)))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentUiState.Loading)

    fun updateStudent(student: Student) = launch { repository.updateStudent(student) }

    fun setStarred(subjectId: Long, starred: Boolean) = launch { repository.setSubjectStarred(subjectId, starred) }

    fun addSubject(name: String) = launch { repository.addSubject(studentId, name) }

    fun renameSubject(subjectId: Long, name: String) = launch { repository.renameSubject(subjectId, name) }

    fun deleteSubject(subjectId: Long) = launch { repository.deleteSubject(subjectId) }

    /** Saves the order the user dragged the subjects into. */
    fun reorderSubjects(subjectIds: List<Long>) = launch { repository.reorderSubjects(subjectIds) }

    /** Moves a taught/learned mark to another day (from the calendar). */
    fun changeEventDate(event: CalendarEvent, date: LocalDate) =
        launch { repository.setLessonDate(event.lessonId, event.kind, date) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
