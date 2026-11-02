package com.example.tutionprogess.data

import androidx.room.TypeConverter
import java.time.LocalDate

/** Dates are stored as epoch days so they sort and compare naturally in SQL. */
class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}
