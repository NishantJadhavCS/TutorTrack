package com.example.tutionprogess.data

import androidx.room.withTransaction
import com.example.tutionprogess.domain.LessonEventKind
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Single entry point to all app data stored in Room. */
class TuitionRepository(
    private val db: AppDatabase,
    private val today: () -> LocalDate = LocalDate::now,
) {
    private val students = db.studentDao()
    private val subjects = db.subjectDao()
    private val lessons = db.lessonDao()

    // Students

    fun observeStudentsWithSubjects(): Flow<List<StudentWithSubjects>> = students.observeAllWithSubjects()

    fun observeStudent(id: Long): Flow<StudentWithSubjects?> = students.observeWithSubjects(id)

    fun observeStudents(): Flow<List<Student>> = students.observeAll()

    suspend fun addStudent(student: Student): Long = students.insert(student.copy(id = 0))

    suspend fun updateStudent(student: Student) = students.update(student)

    /** Also deletes the student's subjects and lessons (cascade). */
    suspend fun deleteStudent(student: Student) = students.delete(student)

    // Subjects

    suspend fun addSubject(studentId: Long, name: String): Long =
        db.withTransaction {
            subjects.insert(
                Subject(studentId = studentId, name = name.trim(), sortOrder = subjects.maxSortOrder(studentId) + 1),
            )
        }

    suspend fun renameSubject(id: Long, name: String) = subjects.rename(id, name.trim())

    suspend fun setSubjectStarred(id: Long, starred: Boolean) = subjects.setStarred(id, starred)

    /** Persists a drag-and-drop order; [subjectIds] are in their new display order. */
    suspend fun reorderSubjects(subjectIds: List<Long>) = subjects.reorder(subjectIds)

    /** Also deletes the subject's lessons (cascade). */
    suspend fun deleteSubject(id: Long) = subjects.delete(id)

    // Lessons

    fun observeSubject(subjectId: Long): Flow<SubjectWithLessons?> = lessons.observeSubjectWithLessons(subjectId)

    suspend fun addLessons(subjectId: Long, count: Int) = lessons.addLessons(subjectId, count)

    /** Saves the title and exact taught/learned dates chosen in the lesson editor. */
    suspend fun updateLessonDetails(id: Long, title: String?, taughtOn: LocalDate?, learnedOn: LocalDate?) =
        lessons.updateDetails(id, title?.trim()?.ifEmpty { null }, taughtOn, learnedOn)

    suspend fun setLessonOptional(id: Long, optional: Boolean) = lessons.setOptional(id, optional)

    /** Ticking saves today's date; unticking clears it. */
    suspend fun setLessonTaught(id: Long, taught: Boolean) = lessons.setTaughtOn(id, if (taught) today() else null)

    suspend fun setLessonLearned(id: Long, learned: Boolean) = lessons.setLearnedOn(id, if (learned) today() else null)

    /** Sets an exact date (or clears it); used by date pickers and "Undo". */
    suspend fun setTaughtDate(id: Long, date: LocalDate?) = lessons.setTaughtOn(id, date)

    suspend fun setLearnedDate(id: Long, date: LocalDate?) = lessons.setLearnedOn(id, date)

    suspend fun setLessonDate(id: Long, kind: LessonEventKind, date: LocalDate?) = when (kind) {
        LessonEventKind.TAUGHT -> setTaughtDate(id, date)
        LessonEventKind.LEARNED -> setLearnedDate(id, date)
    }

    suspend fun deleteLesson(id: Long) = lessons.delete(id)

    /** Re-inserts a deleted lesson with its original id and dates, used by "Undo". */
    suspend fun restoreLesson(lesson: Lesson) = lessons.insertAll(listOf(lesson))

    // Backup

    suspend fun snapshot(): BackupData = db.withTransaction {
        BackupData(students.getAll(), subjects.getAll(), lessons.getAll())
    }

    /** Replaces all data with [data], keeping the original ids so relations stay intact. */
    suspend fun replaceAll(data: BackupData) = db.withTransaction {
        students.deleteAll()
        students.insertAll(data.students)
        subjects.insertAll(data.subjects)
        lessons.insertAll(data.lessons)
    }
}

data class BackupData(
    val students: List<Student>,
    val subjects: List<Subject>,
    val lessons: List<Lesson>,
)
