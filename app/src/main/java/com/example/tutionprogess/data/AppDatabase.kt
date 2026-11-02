package com.example.tutionprogess.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

@Database(
    entities = [Student::class, Subject::class, Lesson::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun subjectDao(): SubjectDao
    abstract fun lessonDao(): LessonDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "tuition_progress.db")
                .addCallback(SeedCallback)
                .build()
    }
}

/** Data inserted once, when the database file is first created (i.e. first launch). */
object SeedData {
    val students: List<Pair<String, List<String>>> = listOf(
        "Atharva" to listOf(
            "English", "Maths", "Science", "History",
            "Geography", "Political Science", "Economics",
        ),
        "Anwisha" to listOf(
            "English", "Maths", "Science", "History",
            "Geography", "Economics",
        ),
    )
}

/**
 * Seeds the database inside Room's creation transaction, so it runs exactly once and
 * can't race with the first query.
 */
private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        val today = LocalDate.now().toEpochDay()
        val now = System.currentTimeMillis()
        SeedData.students.forEachIndexed { index, (name, subjects) ->
            val studentId = db.insert(
                "students",
                SQLiteDatabase.CONFLICT_ABORT,
                ContentValues().apply {
                    put("fullName", name)
                    put("standard", "")
                    put("trackingStartDate", today)
                    putNull("semesterStartDate")
                    put("semesterIsMonthOnly", 0)
                    // Offset keeps the seeded order stable when sorting by createdAt.
                    put("createdAt", now + index)
                },
            )
            subjects.forEachIndexed { order, subject ->
                db.insert(
                    "subjects",
                    SQLiteDatabase.CONFLICT_ABORT,
                    ContentValues().apply {
                        put("studentId", studentId)
                        put("name", subject)
                        put("sortOrder", order)
                        put("isStarred", 0)
                    },
                )
            }
        }
    }
}
