package com.example.tutionprogess.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.tutionprogess.data.Student
import com.example.tutionprogess.ui.components.DateFormats
import com.example.tutionprogess.ui.components.LocalDatePickerDialog
import com.example.tutionprogess.ui.components.MonthYearPickerDialog
import com.example.tutionprogess.ui.components.PickerField
import java.time.LocalDate
import java.time.YearMonth

private enum class PickerTarget { TRACKING, SEMESTER }

/**
 * Bottom sheet for creating ([initial] = null) or editing a student.
 * Dates are kept as epoch days in saveable state so they survive rotation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentSheet(
    initial: Student?,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by rememberSaveable { mutableStateOf(initial?.fullName.orEmpty()) }
    var standard by rememberSaveable { mutableStateOf(initial?.standard.orEmpty()) }
    var trackingDay by rememberSaveable { mutableStateOf((initial?.trackingStartDate ?: LocalDate.now()).toEpochDay()) }
    var semesterDay by rememberSaveable { mutableStateOf(initial?.semesterStartDate?.toEpochDay()) }
    var monthOnly by rememberSaveable { mutableStateOf(initial?.semesterIsMonthOnly ?: false) }
    var picker by rememberSaveable { mutableStateOf<PickerTarget?>(null) }

    val trackingDate = LocalDate.ofEpochDay(trackingDay)
    val semesterDate = semesterDay?.let(LocalDate::ofEpochDay)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(if (initial == null) "Add student" else "Edit student", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = standard,
                onValueChange = { standard = it },
                label = { Text("Standard") },
                placeholder = { Text("e.g. 8th") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PickerField(
                label = "Tracking started",
                value = DateFormats.full(trackingDate),
                onClick = { picker = PickerTarget.TRACKING },
            )

            Text("Semester starts", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !monthOnly,
                    onClick = { monthOnly = false },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Exact date") }
                SegmentedButton(
                    selected = monthOnly,
                    onClick = { monthOnly = true },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Month & year") }
            }
            PickerField(
                label = if (monthOnly) "Month" else "Date",
                value = semesterDate?.let { DateFormats.semester(it, monthOnly) } ?: "Not set",
                icon = Icons.Rounded.Event,
                onClick = { picker = PickerTarget.SEMESTER },
                trailing = if (semesterDate != null) {
                    { TextButton(onClick = { semesterDay = null }) { Text("Clear") } }
                } else {
                    null
                },
            )

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        val semester = semesterDate?.let { if (monthOnly) it.withDayOfMonth(1) else it }
                        val base = initial ?: Student(fullName = "", trackingStartDate = trackingDate)
                        onSave(
                            base.copy(
                                fullName = name.trim(),
                                standard = standard.trim(),
                                trackingStartDate = trackingDate,
                                semesterStartDate = semester,
                                semesterIsMonthOnly = monthOnly,
                            ),
                        )
                    },
                ) { Text("Save") }
            }
        }
    }

    when (picker) {
        PickerTarget.TRACKING -> LocalDatePickerDialog(
            initial = trackingDate,
            onDismiss = { picker = null },
            onConfirm = { trackingDay = it.toEpochDay(); picker = null },
        )
        PickerTarget.SEMESTER -> if (monthOnly) {
            MonthYearPickerDialog(
                initial = semesterDate?.let(YearMonth::from),
                onDismiss = { picker = null },
                onConfirm = { semesterDay = it.atDay(1).toEpochDay(); picker = null },
            )
        } else {
            LocalDatePickerDialog(
                initial = semesterDate,
                onDismiss = { picker = null },
                onConfirm = { semesterDay = it.toEpochDay(); picker = null },
            )
        }
        null -> Unit
    }
}

