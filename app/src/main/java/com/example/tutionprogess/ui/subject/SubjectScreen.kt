package com.example.tutionprogess.ui.subject

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tutionprogess.data.Lesson
import com.example.tutionprogess.domain.SubjectProgress
import com.example.tutionprogess.domain.SubjectStatus
import com.example.tutionprogess.ui.AppViewModelProvider
import com.example.tutionprogess.ui.components.BackButton
import com.example.tutionprogess.ui.components.DateFormats
import com.example.tutionprogess.ui.components.LocalDatePickerDialog
import com.example.tutionprogess.domain.LessonEventKind
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import java.time.LocalDate
import com.example.tutionprogess.ui.components.EmptyState
import com.example.tutionprogess.ui.components.LabeledProgressBar
import com.example.tutionprogess.ui.components.StatusBadge
import com.example.tutionprogess.ui.components.AccentBarRow
import com.example.tutionprogess.ui.components.style
import com.example.tutionprogess.ui.components.tintedCardColor
import kotlinx.coroutines.flow.collectLatest

/** "Lesson N - Title", or "<Subject> Lesson N" when there's no title. */
fun Lesson.displayName(subjectName: String): String =
    title?.takeIf { it.isNotBlank() }?.let { "Lesson $number - $it" } ?: "$subjectName Lesson $number"

private const val MAX_LESSONS_AT_ONCE = 200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectScreen(
    onBack: () -> Unit,
    viewModel: SubjectViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    // "edit:<id>", "date:<id>:<TAUGHT|LEARNED>", or "add"; a String so it survives rotation.
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // A new change replaces the snackbar that's showing (collectLatest cancels the old one).
    LaunchedEffect(viewModel) {
        viewModel.changes.collectLatest { change ->
            val result = snackbar.showSnackbar(change.message, actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) change.undo()
        }
    }
    if (state == SubjectUiState.Missing) {
        LaunchedEffect(Unit) { onBack() }
    }

    val loaded = state as? SubjectUiState.Loaded
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(loaded?.progress?.subject?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (loaded != null) {
                        IconButton(onClick = { viewModel.addLessons(1) }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add one lesson")
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (loaded != null) {
                ExtendedFloatingActionButton(
                    onClick = { dialog = "add" },
                    icon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                    text = { Text("Add lessons") },
                )
            }
        },
    ) { padding ->
        if (loaded == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val subject = loaded.progress
        val subjectName = subject.subject.name

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SubjectHeader(subject, loaded.hasSemester) }
            if (subject.lessons.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Rounded.LibraryBooks,
                        title = "No lessons yet",
                        message = "Tap Add lessons to create them all at once.",
                    )
                }
            } else {
                item {
                    Text(
                        "Lessons",
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            items(subject.lessons, key = { it.id }) { lesson ->
                val label = lesson.displayName(subjectName)
                LessonRow(
                    lesson = lesson,
                    label = label,
                    onTaught = { viewModel.setTaught(lesson, it, "Lesson ${lesson.number}") },
                    onLearned = { viewModel.setLearned(lesson, it, "Lesson ${lesson.number}") },
                    onEdit = { dialog = "edit:${lesson.id}" },
                    onChangeDate = { kind -> dialog = "date:${lesson.id}:${kind.name}" },
                    onToggleOptional = { viewModel.setOptional(lesson.id, !lesson.isOptional) },
                    onDelete = { viewModel.delete(lesson, "Lesson ${lesson.number}") },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        val close = { dialog = null }
        val current = dialog
        when {
            current == "add" -> AddLessonsDialog(
                nextNumber = (subject.lessons.maxOfOrNull { it.number } ?: 0) + 1,
                onDismiss = close,
                onConfirm = { viewModel.addLessons(it); close() },
            )
            current?.startsWith("date:") == true -> {
                val parts = current.split(':')
                val lesson = subject.lessons.find { it.id == parts.getOrNull(1)?.toLongOrNull() }
                val kind = LessonEventKind.entries.find { it.name == parts.getOrNull(2) }
                val date = if (kind == LessonEventKind.TAUGHT) lesson?.taughtOn else lesson?.learnedOn
                if (lesson == null || kind == null || date == null) {
                    LaunchedEffect(Unit) { close() }
                } else {
                    val today = LocalDate.now()
                    LocalDatePickerDialog(
                        initial = date,
                        minDate = minOf(loaded.trackingStart, today),
                        maxDate = today,
                        onDismiss = close,
                        onConfirm = { viewModel.changeDate(lesson, kind, it); close() },
                    )
                }
            }
            current?.startsWith("edit:") == true -> {
                val lesson = subject.lessons.find { it.id == current.substringAfter(':').toLongOrNull() }
                if (lesson == null) {
                    LaunchedEffect(Unit) { close() }
                } else {
                    EditLessonSheet(
                        lesson = lesson,
                        subjectName = subjectName,
                        trackingStart = loaded.trackingStart,
                        onDismiss = close,
                        onSave = { title, taughtOn, learnedOn ->
                            viewModel.saveDetails(lesson.id, title, taughtOn, learnedOn)
                            close()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectHeader(subject: SubjectProgress, hasSemester: Boolean) {
    val counts = subject.counts
    val optional = subject.lessons.count { it.isOptional }
    Card(
        colors = CardDefaults.cardColors(containerColor = tintedCardColor()),
        shape = MaterialTheme.shapes.large,
    ) {
        AccentBarRow(subject.status.style().color) {
            Column(Modifier.weight(1f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(subject.status)
                    Box(Modifier.weight(1f))
                    Text(
                        "${counts.leftToTeach} to teach · ${counts.leftToLearn} to learn",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LabeledProgressBar("Taught", counts.taught, counts.total, MaterialTheme.colorScheme.primary)
                LabeledProgressBar("Learned", counts.learned, counts.total, MaterialTheme.colorScheme.tertiary)
                val note = when {
                    subject.status == SubjectStatus.NONE && counts.total > 0 && !hasSemester ->
                        "Set the semester date on the student screen to see if this subject is on track."
                    optional > 0 -> "$optional optional lesson${if (optional == 1) "" else "s"} not counted."
                    else -> null
                }
                note?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonRow(
    lesson: Lesson,
    label: String,
    onTaught: (Boolean) -> Unit,
    onLearned: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onChangeDate: (LessonEventKind) -> Unit,
    onToggleOptional: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(if (lesson.isOptional) 0.6f else 1f, label = "optionalAlpha")
    val container by animateColorAsState(
        if (lesson.isOptional) MaterialTheme.colorScheme.surface else tintedCardColor(),
        label = "optionalContainer",
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        border = if (lesson.isOptional) CardDefaults.outlinedCardBorder() else null,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 14.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    modifier = Modifier.weight(1f).alpha(contentAlpha),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (lesson.isOptional) OptionalLabel()
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit lesson ${lesson.number}")
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More options for lesson ${lesson.number}")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(if (lesson.isOptional) "Mark as not optional" else "Mark as optional") },
                            leadingIcon = {
                                Icon(
                                    if (lesson.isOptional) Icons.Rounded.TaskAlt else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                )
                            },
                            onClick = { menuOpen = false; onToggleOptional() },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
            Row(Modifier.alpha(contentAlpha), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipWithDate(lesson.taughtOn, onChangeDate = { onChangeDate(LessonEventKind.TAUGHT) }, kindLabel = "taught") {
                    ToggleChip("Taught", lesson.isTaught, onTaught, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                }
                ChipWithDate(lesson.learnedOn, onChangeDate = { onChangeDate(LessonEventKind.LEARNED) }, kindLabel = "learned") {
                    ToggleChip("Learned", lesson.isLearned, onLearned, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    selected: Boolean,
    onChange: (Boolean) -> Unit,
    selectedContainer: androidx.compose.ui.graphics.Color,
    selectedContent: androidx.compose.ui.graphics.Color,
) {
    FilterChip(
        selected = selected,
        onClick = { onChange(!selected) },
        label = { Text(label) },
        leadingIcon = {
            Icon(
                if (selected) Icons.Rounded.Check else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedContainer,
            selectedLabelColor = selectedContent,
            selectedLeadingIconColor = selectedContent,
        ),
        shape = CircleShape,
    )
}

@Composable
private fun OptionalLabel() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Text("Optional", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}

/** A toggle chip with its date underneath when ticked; tapping the date opens a date picker. */
@Composable
private fun ChipWithDate(
    date: LocalDate?,
    onChangeDate: () -> Unit,
    kindLabel: String,
    chip: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        chip()
        AnimatedVisibility(visible = date != null) {
            val shown = date ?: return@AnimatedVisibility
            Row(
                Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClickLabel = "Change $kindLabel date", onClick = onChangeDate)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    DateFormats.short(shown),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    Icons.Outlined.EditCalendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 3.dp).size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun AddLessonsDialog(nextNumber: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val count = text.toIntOrNull()
    val valid = count != null && count in 1..MAX_LESSONS_AT_ONCE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How many lessons?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { new -> text = new.filter(Char::isDigit).take(3) },
                    label = { Text("Number of lessons") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    when {
                        count == null || count == 0 -> "New lessons start at Lesson $nextNumber."
                        !valid -> "You can add up to $MAX_LESSONS_AT_ONCE at a time."
                        count == 1 -> "Adds Lesson $nextNumber."
                        else -> "Adds Lesson $nextNumber to Lesson ${nextNumber + count - 1}."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (count != null && count > MAX_LESSONS_AT_ONCE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { count?.let(onConfirm) }, enabled = valid) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
