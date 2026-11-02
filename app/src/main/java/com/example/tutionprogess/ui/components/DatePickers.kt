package com.example.tutionprogess.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Material date picker dialog that works in [LocalDate] (the picker uses UTC midnight millis).
 * Only dates in [minDate]..[maxDate] (each optional, inclusive) can be picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalDatePickerDialog(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    fun allowed(date: LocalDate) = (minDate == null || !date.isBefore(minDate)) && (maxDate == null || !date.isAfter(maxDate))
    val start = (initial ?: LocalDate.now()).let {
        when {
            minDate != null && it.isBefore(minDate) -> minDate
            maxDate != null && it.isAfter(maxDate) -> maxDate
            else -> it
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = start.toEpochDay() * MILLIS_PER_DAY,
        yearRange = (minDate?.year ?: DatePickerDefaults.YearRange.first)..(maxDate?.year ?: DatePickerDefaults.YearRange.last),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = allowed(LocalDate.ofEpochDay(utcTimeMillis / MILLIS_PER_DAY))

            override fun isSelectableYear(year: Int) =
                (minDate == null || year >= minDate.year) && (maxDate == null || year <= maxDate.year)
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(LocalDate.ofEpochDay(it / MILLIS_PER_DAY)) } },
                enabled = state.selectedDateMillis != null,
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

/** Picks just a month and year. */
@Composable
fun MonthYearPickerDialog(
    initial: YearMonth?,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit,
) {
    val start = initial ?: YearMonth.now()
    var year by rememberSaveable { mutableIntStateOf(start.year) }
    var month by rememberSaveable { mutableIntStateOf(start.monthValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Semester month") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { year-- }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous year")
                    }
                    Text(
                        year.toString(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    IconButton(onClick = { year++ }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next year")
                    }
                }
                for (row in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (col in 0 until 3) {
                            val m = row * 3 + col + 1
                            val selected = m == month
                            Surface(
                                onClick = { month = m },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .semantics { this.selected = selected },
                                shape = MaterialTheme.shapes.small,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        Month.of(m).getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(YearMonth.of(year, month)) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        modifier = Modifier.fillMaxWidth(),
    )
}
