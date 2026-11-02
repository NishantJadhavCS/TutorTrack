package com.example.tutionprogess.ui.subject

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
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import com.example.tutionprogess.data.Lesson
import com.example.tutionprogess.ui.components.DateFormats
import com.example.tutionprogess.ui.components.LocalDatePickerDialog
import com.example.tutionprogess.ui.components.PickerField
import java.time.LocalDate

private enum class LessonDate { TAUGHT, LEARNED }

/**
 * Edits a lesson's title and its taught/learned dates.
 * Dates can be anything from the student's [trackingStart] up to today, or cleared.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLessonSheet(
    lesson: Lesson,
    subjectName: String,
    trackingStart: LocalDate,
    onDismiss: () -> Unit,
    onSave: (title: String, taughtOn: LocalDate?, learnedOn: LocalDate?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by rememberSaveable { mutableStateOf(lesson.title.orEmpty()) }
    // Epoch days so the values survive rotation.
    var taughtDay by rememberSaveable { mutableStateOf(lesson.taughtOn?.toEpochDay()) }
    var learnedDay by rememberSaveable { mutableStateOf(lesson.learnedOn?.toEpochDay()) }
    var picker by rememberSaveable { mutableStateOf<LessonDate?>(null) }

    val taught = taughtDay?.let(LocalDate::ofEpochDay)
    val learned = learnedDay?.let(LocalDate::ofEpochDay)
    val today = LocalDate.now()
    // If tracking was set to start in the future, still allow today so the picker isn't empty.
    val minDate = minOf(trackingStart, today)

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
            Text("Edit lesson ${lesson.number}", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title (optional)") },
                placeholder = { Text("$subjectName Lesson ${lesson.number}") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            PickerField(
                label = "Taught on",
                value = taught?.let(DateFormats::full) ?: "Not taught yet",
                icon = Icons.Rounded.School,
                onClick = { picker = LessonDate.TAUGHT },
                trailing = taught?.let { { TextButton(onClick = { taughtDay = null }) { Text("Clear") } } },
            )
            PickerField(
                label = "Learned on",
                value = learned?.let(DateFormats::full) ?: "Not learned yet",
                icon = Icons.Rounded.TaskAlt,
                onClick = { picker = LessonDate.LEARNED },
                trailing = learned?.let { { TextButton(onClick = { learnedDay = null }) { Text("Clear") } } },
            )
            Text(
                "Dates can be from ${DateFormats.full(minDate)} (tracking start) up to today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSave(title, taught, learned) }) { Text("Save") }
            }
        }
    }

    picker?.let { target ->
        LocalDatePickerDialog(
            initial = if (target == LessonDate.TAUGHT) taught else learned,
            minDate = minDate,
            maxDate = today,
            onDismiss = { picker = null },
            onConfirm = { date ->
                if (target == LessonDate.TAUGHT) taughtDay = date.toEpochDay() else learnedDay = date.toEpochDay()
                picker = null
            },
        )
    }
}
