package com.example.tutionprogess.ui.subject

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.tutionprogess.data.Lesson
import com.example.tutionprogess.data.TuitionRepository
import com.example.tutionprogess.domain.LessonEventKind
import com.example.tutionprogess.domain.StudentProgress
import com.example.tutionprogess.ui.components.DateFormats
import com.example.tutionprogess.domain.SubjectProgress
import com.example.tutionprogess.ui.navigation.SubjectRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface SubjectUiState {
    data object Loading : SubjectUiState
    data object Missing : SubjectUiState
    data class Loaded(
        val progress: SubjectProgress,
        val hasSemester: Boolean,
        /** Earliest date allowed when editing taught/learned dates. */
        val trackingStart: LocalDate,
    ) : SubjectUiState
}

/** A change that can be reverted from the snackbar. */
class UndoableChange(val message: String, val undo: () -> Unit)

@OptIn(ExperimentalCoroutinesApi::class)
class SubjectViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: TuitionRepository,
) : ViewModel() {
    private val subjectId = savedStateHandle.toRoute<SubjectRoute>().subjectId

    private val _changes = Channel<UndoableChange>(Channel.BUFFERED)
    val changes: Flow<UndoableChange> = _changes.receiveAsFlow()

    // Status depends on the student's semester dates, so observe the whole student.
    val uiState: StateFlow<SubjectUiState> = repository.observeSubject(subjectId)
        .map { it?.subject?.studentId }
        .distinctUntilChanged()
        .flatMapLatest { studentId ->
            if (studentId == null) flowOf(SubjectUiState.Missing)
            else repository.observeStudent(studentId).map { data ->
                val progress = data?.let { StudentProgress.of(it, LocalDate.now()) }
                val subject = progress?.subjects?.find { it.subject.id == subjectId }
                if (subject == null) SubjectUiState.Missing
                else SubjectUiState.Loaded(
                    progress = subject,
                    hasSemester = progress.schedule != null,
                    trackingStart = progress.student.trackingStartDate,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubjectUiState.Loading)

    fun setTaught(lesson: Lesson, taught: Boolean, label: String) = launch {
        repository.setLessonTaught(lesson.id, taught)
        offerUndo(if (taught) "$label marked as taught" else "$label unmarked as taught") {
            repository.setTaughtDate(lesson.id, lesson.taughtOn)
        }
    }

    fun setLearned(lesson: Lesson, learned: Boolean, label: String) = launch {
        repository.setLessonLearned(lesson.id, learned)
        offerUndo(if (learned) "$label marked as learned" else "$label unmarked as learned") {
            repository.setLearnedDate(lesson.id, lesson.learnedOn)
        }
    }

    /** Moves an existing taught/learned mark to [date], with Undo. */
    fun changeDate(lesson: Lesson, kind: LessonEventKind, date: LocalDate) = launch {
        val previous = if (kind == LessonEventKind.TAUGHT) lesson.taughtOn else lesson.learnedOn
        if (previous == date) return@launch
        repository.setLessonDate(lesson.id, kind, date)
        val what = if (kind == LessonEventKind.TAUGHT) "Taught" else "Learned"
        offerUndo("Lesson ${lesson.number}: $what date set to ${DateFormats.short(date)}") {
            repository.setLessonDate(lesson.id, kind, previous)
        }
    }

    fun saveDetails(lessonId: Long, title: String, taughtOn: LocalDate?, learnedOn: LocalDate?) = launch {
        repository.updateLessonDetails(lessonId, title, taughtOn, learnedOn)
    }

    fun setOptional(lessonId: Long, optional: Boolean) = launch { repository.setLessonOptional(lessonId, optional) }

    fun delete(lesson: Lesson, label: String) = launch {
        repository.deleteLesson(lesson.id)
        offerUndo("$label deleted") { repository.restoreLesson(lesson) }
    }

    fun addLessons(count: Int) = launch { repository.addLessons(subjectId, count) }

    private suspend fun offerUndo(message: String, revert: suspend () -> Unit) {
        _changes.send(UndoableChange(message) { launch(revert) })
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
