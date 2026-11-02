package com.example.tutionprogess.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val standard: String = "",
    val trackingStartDate: LocalDate,
    val semesterStartDate: LocalDate? = null,
    /** If true, only month/year is shown and the deadline is the 1st of that month. */
    val semesterIsMonthOnly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** The date counted as the semester deadline (1st of the month for month-only dates). */
    val semesterDeadline: LocalDate?
        get() = semesterStartDate?.let { if (semesterIsMonthOnly) it.withDayOfMonth(1) else it }
}

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("studentId")],
)
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val name: String,
    val sortOrder: Int,
    val isStarred: Boolean = false,
)

@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subjectId")],
)
data class Lesson(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val number: Int,
    val title: String? = null,
    val isOptional: Boolean = false,
    val taughtOn: LocalDate? = null,
    val learnedOn: LocalDate? = null,
) {
    val isTaught: Boolean get() = taughtOn != null
    val isLearned: Boolean get() = learnedOn != null
}

data class SubjectWithLessons(
    @Embedded val subject: Subject,
    @Relation(parentColumn = "id", entityColumn = "subjectId")
    val lessons: List<Lesson>,
)

data class StudentWithSubjects(
    @Embedded val student: Student,
    @Relation(entity = Subject::class, parentColumn = "id", entityColumn = "studentId")
    val subjects: List<SubjectWithLessons>,
)
