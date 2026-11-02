package com.example.tutionprogess.ui.student

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tutionprogess.domain.StudentProgress
import com.example.tutionprogess.domain.SubjectProgress
import com.example.tutionprogess.ui.AppViewModelProvider
import com.example.tutionprogess.ui.components.BackButton
import com.example.tutionprogess.ui.components.ConfirmDialog
import com.example.tutionprogess.ui.components.DateFormats
import com.example.tutionprogess.ui.components.EmptyState
import com.example.tutionprogess.ui.components.LabeledProgressBar
import com.example.tutionprogess.ui.components.StatusBadge
import com.example.tutionprogess.ui.components.AccentBarRow
import com.example.tutionprogess.ui.components.style
import com.example.tutionprogess.ui.components.tintedCardColor
import com.example.tutionprogess.ui.components.TextInputDialog
import com.example.tutionprogess.ui.calendar.EventKindFilter
import com.example.tutionprogess.ui.calendar.KindFilterChips
import com.example.tutionprogess.ui.calendar.LessonCalendar
import com.example.tutionprogess.ui.home.daysLeftText
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Which dialog is open; ids are saved so dialogs survive rotation. */
private sealed interface StudentDialog {
    data object EditStudent : StudentDialog
    data object AddSubject : StudentDialog
    data class Rename(val subjectId: Long) : StudentDialog
    data class Delete(val subjectId: Long) : StudentDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentScreen(
    onBack: () -> Unit,
    onOpenSubject: (Long) -> Unit,
    viewModel: StudentViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Encoded as "kind:id" so it can live in rememberSaveable.
    var dialogKey by rememberSaveable { mutableStateOf<String?>(null) }
    val dialog = dialogKey.toDialog()
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var kindFilter by rememberSaveable { mutableStateOf(EventKindFilter.BOTH) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val dbSubjects = (state as? StudentUiState.Loaded)?.progress?.subjects.orEmpty()
    // Order shown while dragging and until Room confirms it; null means "use Room's order".
    var localOrder by remember { mutableStateOf<List<SubjectProgress>?>(null) }
    val subjects = localOrder ?: dbSubjects
    LaunchedEffect(dbSubjects) {
        if (localOrder?.map { it.subject.id } == dbSubjects.map { it.subject.id }) localOrder = null
    }
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val current = (localOrder ?: dbSubjects).toMutableList()
        val fromIndex = current.indexOfFirst { it.subject.id == from.key }
        val toIndex = current.indexOfFirst { it.subject.id == to.key }
        if (fromIndex >= 0 && toIndex >= 0) {
            current.add(toIndex, current.removeAt(fromIndex))
            localOrder = current
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }
    val onDragStopped = {
        val order = localOrder?.map { it.subject.id }
        if (order == null || order == dbSubjects.map { it.subject.id }) {
            localOrder = null
        } else {
            viewModel.reorderSubjects(order)
        }
    }

    if (state == StudentUiState.Missing) {
        LaunchedEffect(Unit) { onBack() }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(title = {}, navigationIcon = { BackButton(onBack) }, scrollBehavior = scrollBehavior)
        },
        floatingActionButton = {
            if (state is StudentUiState.Loaded && !showCalendar) {
                ExtendedFloatingActionButton(
                    onClick = { dialogKey = StudentDialog.AddSubject.key() },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Add subject") },
                )
            }
        },
    ) { padding ->
        val loaded = state as? StudentUiState.Loaded
        if (loaded == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val progress = loaded.progress

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(progress, onEdit = { dialogKey = StudentDialog.EditStudent.key() }) }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !showCalendar,
                        onClick = { showCalendar = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("Subjects") }
                    SegmentedButton(
                        selected = showCalendar,
                        onClick = { showCalendar = true },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("Calendar") }
                }
            }
            if (showCalendar) {
                item { KindFilterChips(kindFilter, onSelect = { kindFilter = it }) }
                item {
                    val events = remember(loaded.events, kindFilter) { loaded.events.filter { kindFilter.includes(it.kind) } }
                    LessonCalendar(
                        events = events,
                        showStudent = false,
                        onOpenEvent = { onOpenSubject(it.subjectId) },
                        onChangeDate = viewModel::changeEventDate,
                    )
                }
                return@LazyColumn
            }
            item { SemesterBanner(progress, onSetDate = { dialogKey = StudentDialog.EditStudent.key() }) }
            item {
                Row(Modifier.padding(start = 4.dp, top = 12.dp), verticalAlignment = Alignment.Bottom) {
                    Text("Subjects", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    if (subjects.size > 1) {
                        Text(
                            "Long-press to reorder",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (subjects.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        title = "No subjects yet",
                        message = "Tap Add subject to get started.",
                    )
                }
            }
            items(subjects, key = { it.subject.id }) { subject ->
                ReorderableItem(reorderState, key = subject.subject.id) { isDragging ->
                    SubjectRow(
                        subject = subject,
                        isDragging = isDragging,
                        onClick = { onOpenSubject(subject.subject.id) },
                        onToggleStar = { viewModel.setStarred(subject.subject.id, !subject.subject.isStarred) },
                        onRename = { dialogKey = StudentDialog.Rename(subject.subject.id).key() },
                        onDelete = { dialogKey = StudentDialog.Delete(subject.subject.id).key() },
                        modifier = Modifier.longPressDraggableHandle(
                            onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
                            onDragStopped = {
                                haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                onDragStopped()
                            },
                        ),
                    )
                }
            }
        }

        val close = { dialogKey = null }
        when (dialog) {
            StudentDialog.EditStudent -> EditStudentSheet(
                initial = progress.student,
                onDismiss = close,
                onSave = { viewModel.updateStudent(it); close() },
            )
            StudentDialog.AddSubject -> TextInputDialog(
                title = "Add subject",
                label = "Subject name",
                confirmText = "Add",
                onDismiss = close,
                onConfirm = { viewModel.addSubject(it); close() },
            )
            is StudentDialog.Rename -> {
                val subject = progress.subjects.find { it.subject.id == dialog.subjectId }?.subject
                if (subject == null) LaunchedEffect(Unit) { close() } else TextInputDialog(
                    title = "Rename subject",
                    label = "Subject name",
                    initial = subject.name,
                    onDismiss = close,
                    onConfirm = { viewModel.renameSubject(subject.id, it); close() },
                )
            }
            is StudentDialog.Delete -> {
                val subject = progress.subjects.find { it.subject.id == dialog.subjectId }
                if (subject == null) LaunchedEffect(Unit) { close() } else ConfirmDialog(
                    title = "Delete ${subject.subject.name}?",
                    message = "This also deletes its ${subject.lessons.size} lesson(s). This can't be undone.",
                    confirmText = "Delete",
                    onDismiss = close,
                    onConfirm = { viewModel.deleteSubject(subject.subject.id); close() },
                )
            }
            null -> Unit
        }
    }
}

private fun StudentDialog.key(): String = when (this) {
    StudentDialog.EditStudent -> "edit"
    StudentDialog.AddSubject -> "add"
    is StudentDialog.Rename -> "rename:$subjectId"
    is StudentDialog.Delete -> "delete:$subjectId"
}

private fun String?.toDialog(): StudentDialog? {
    if (this == null) return null
    val id = substringAfter(':', "").toLongOrNull()
    return when (substringBefore(':')) {
        "edit" -> StudentDialog.EditStudent
        "add" -> StudentDialog.AddSubject
        "rename" -> id?.let(StudentDialog::Rename)
        "delete" -> id?.let(StudentDialog::Delete)
        else -> null
    }
}

@Composable
private fun Header(progress: StudentProgress, onEdit: () -> Unit) {
    Row(Modifier.padding(start = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(progress.student.fullName, style = MaterialTheme.typography.headlineMedium)
            Text(
                progress.student.standard.ifBlank { "Standard not set" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit student") }
    }
}

@Composable
private fun SemesterBanner(progress: StudentProgress, onSetDate: () -> Unit) {
    val schedule = progress.schedule
    if (schedule == null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = tintedCardColor(MaterialTheme.colorScheme.surfaceContainerHigh)),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Set the semester start", Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "Add the date to see teaching days left, the pace you need and whether each subject is on track.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onSetDate, modifier = Modifier.padding(top = 4.dp)) { Text("Set date") }
            }
        }
        return
    }

    val totals = progress.totals
    val date = DateFormats.semester(schedule.deadline, progress.student.semesterIsMonthOnly)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Semester starts $date", style = MaterialTheme.typography.labelLarge)
            Text(daysLeftText(schedule.daysLeft), style = MaterialTheme.typography.headlineSmall)
            Row(Modifier.fillMaxWidth()) {
                BannerStat("${totals.leftToTeach}", "lessons left to teach", Modifier.weight(1f))
                BannerStat("${totals.leftToLearn}", "lessons left to learn", Modifier.weight(1f))
            }
            progress.paceText?.let { pace ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Pace needed: $pace", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun BannerStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SubjectRow(
    subject: SubjectProgress,
    isDragging: Boolean,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val counts = subject.counts
    var menuOpen by remember { mutableStateOf(false) }
    // Lifted look while dragging: shadow, slight scale and a brighter surface.
    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "dragElevation")
    val scale by animateFloatAsState(if (isDragging) 1.03f else 1f, label = "dragScale")
    val container by animateColorAsState(
        if (isDragging) tintedCardColor(MaterialTheme.colorScheme.surfaceContainerHigh) else tintedCardColor(),
        label = "dragContainer",
    )
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = MaterialTheme.shapes.medium,
    ) {
        AccentBarRow(subject.status.style().color) {
            Column(Modifier.weight(1f).padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 16.dp).animateContentSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(subject.subject.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconToggleButton(checked = subject.subject.isStarred, onCheckedChange = { onToggleStar() }) {
                        Icon(
                            if (subject.subject.isStarred) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = if (subject.subject.isStarred) "Unstar" else "Star",
                            tint = if (subject.subject.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More options for ${subject.subject.name}")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
                                onClick = { menuOpen = false; onRename() },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                onClick = { menuOpen = false; onDelete() },
                            )
                        }
                    }
                }
                Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(subject.status)
                        Box(Modifier.weight(1f))
                        Text(
                            when {
                                counts.total == 0 -> "No lessons yet"
                                counts.leftToTeach == 0 -> "All taught"
                                else -> "${counts.leftToTeach} left to teach"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LabeledProgressBar("Taught", counts.taught, counts.total, MaterialTheme.colorScheme.primary)
                    LabeledProgressBar("Learned", counts.learned, counts.total, MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}
