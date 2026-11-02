package com.example.tutionprogess.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface StudentDao {
    @Transaction
    @Query("SELECT * FROM students ORDER BY createdAt, id")
    fun observeAllWithSubjects(): Flow<List<StudentWithSubjects>>

    @Transaction
    @Query("SELECT * FROM students WHERE id = :id")
    fun observeWithSubjects(id: Long): Flow<StudentWithSubjects?>

    @Query("SELECT * FROM students ORDER BY createdAt, id")
    fun observeAll(): Flow<List<Student>>

    @Query("SELECT * FROM students ORDER BY createdAt, id")
    suspend fun getAll(): List<Student>

    @Insert
    suspend fun insert(student: Student): Long

    @Insert
    suspend fun insertAll(students: List<Student>)

    @Update
    suspend fun update(student: Student)

    @Delete
    suspend fun delete(student: Student)

    @Query("DELETE FROM students")
    suspend fun deleteAll()
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY studentId, sortOrder, id")
    suspend fun getAll(): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun get(id: Long): Subject?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM subjects WHERE studentId = :studentId")
    suspend fun maxSortOrder(studentId: Long): Int

    @Insert
    suspend fun insert(subject: Subject): Long

    @Insert
    suspend fun insertAll(subjects: List<Subject>)

    @Query("UPDATE subjects SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE subjects SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Int)

    /** Saves a manual order: each subject's sortOrder becomes its position in [ids]. */
    @Transaction
    suspend fun reorder(ids: List<Long>) {
        ids.forEachIndexed { index, id -> setSortOrder(id, index) }
    }

    @Query("UPDATE subjects SET isStarred = :starred WHERE id = :id")
    suspend fun setStarred(id: Long, starred: Boolean)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface LessonDao {
    @Transaction
    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    fun observeSubjectWithLessons(subjectId: Long): Flow<SubjectWithLessons?>

    @Query("SELECT * FROM lessons ORDER BY subjectId, number, id")
    suspend fun getAll(): List<Lesson>

    @Query("SELECT COALESCE(MAX(number), 0) FROM lessons WHERE subjectId = :subjectId")
    suspend fun maxNumber(subjectId: Long): Int

    @Insert
    suspend fun insertAll(lessons: List<Lesson>)

    @Query("UPDATE lessons SET title = :title, taughtOn = :taughtOn, learnedOn = :learnedOn WHERE id = :id")
    suspend fun updateDetails(id: Long, title: String?, taughtOn: LocalDate?, learnedOn: LocalDate?)

    @Query("UPDATE lessons SET isOptional = :optional WHERE id = :id")
    suspend fun setOptional(id: Long, optional: Boolean)

    @Query("UPDATE lessons SET taughtOn = :date WHERE id = :id")
    suspend fun setTaughtOn(id: Long, date: LocalDate?)

    @Query("UPDATE lessons SET learnedOn = :date WHERE id = :id")
    suspend fun setLearnedOn(id: Long, date: LocalDate?)

    @Query("DELETE FROM lessons WHERE id = :id")
    suspend fun delete(id: Long)

    /** Appends [count] lessons numbered after the current last lesson. */
    @Transaction
    suspend fun addLessons(subjectId: Long, count: Int) {
        if (count <= 0) return
        val start = maxNumber(subjectId) + 1
        insertAll((start until start + count).map { Lesson(subjectId = subjectId, number = it) })
    }
}
