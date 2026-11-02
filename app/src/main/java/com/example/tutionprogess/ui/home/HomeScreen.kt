package com.example.tutionprogess.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tutionprogess.domain.StudentProgress
import com.example.tutionprogess.ui.AppViewModelProvider
import com.example.tutionprogess.ui.components.DateFormats
import com.example.tutionprogess.ui.components.EmptyState
import com.example.tutionprogess.ui.calendar.EventKindFilter
import com.example.tutionprogess.ui.calendar.KindFilterChips
import com.example.tutionprogess.ui.calendar.LessonCalendar
import com.example.tutionprogess.ui.components.CountUpText
import com.example.tutionprogess.ui.components.ProgressRing
import com.example.tutionprogess.ui.components.tintedCardColor
import com.example.tutionprogess.ui.theme.StatNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenStudent: (Long) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSubject: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // Calendar filters: null student = all students.
    var studentFilter by rememberSaveable { mutableStateOf<Long?>(null) }
    var kindFilter by rememberSaveable { mutableStateOf(EventKindFilter.BOTH) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    val s = state
                    val name = s?.teacherName.orEmpty()
                    Text(
                        if (s == null) "" else if (name.isBlank()) s.greeting else "${s.greeting}, $name",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenProfile) { Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val s = state
        if (s == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    DateFormats.weekday(s.today),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (s.students.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Groups,
                        title = "No students yet",
                        message = "Add a student from Settings to start tracking progress.",
                    )
                }
            } else {
                item { OverviewCard(s) }
                item {
                    Text(
                        "Students",
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(s.students, key = { it.student.id }) { student ->
                    StudentCard(student, onClick = { onOpenStudent(student.student.id) })
                }

                item {
                    Text(
                        "Calendar",
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                item {
                    // A filtered-out student that was deleted falls back to "All".
                    val activeStudent = studentFilter?.takeIf { id -> s.students.any { it.student.id == id } }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChip(selected = activeStudent == null, onClick = { studentFilter = null }, label = { Text("All") })
                            s.students.forEach { student ->
                                FilterChip(
                                    selected = activeStudent == student.student.id,
                                    onClick = { studentFilter = student.student.id },
                                    label = { Text(student.student.fullName) },
                                )
                            }
                        }
                        KindFilterChips(kindFilter, onSelect = { kindFilter = it })
                    }
                }
                item {
                    val activeStudent = studentFilter
                    val events = remember(s.events, activeStudent, kindFilter) {
                        s.events.filter { (activeStudent == null || it.studentId == activeStudent) && kindFilter.includes(it.kind) }
                    }
                    LessonCalendar(
                        events = events,
                        showStudent = true,
                        onOpenEvent = { onOpenSubject(it.subjectId) },
                        onChangeDate = viewModel::changeEventDate,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(state: HomeUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 20.dp)) {
            val totals = state.totals
            Stat("Total", totals.total, Modifier.weight(1f))
            Stat("Taught", totals.taught, Modifier.weight(1f))
            Stat("Learned", totals.learned, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier.clearAndSetSemantics { contentDescription = "$value $label" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CountUpText(value, style = StatNumber.copy(fontSize = 36.sp, lineHeight = 40.sp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun StudentCard(progress: StudentProgress, onClick: () -> Unit) {
    val student = progress.student
    val totals = progress.totals

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = tintedCardColor()),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(student.fullName, style = MaterialTheme.typography.titleLarge)
                Text(
                    student.standard.ifBlank { "Standard not set" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    progress.schedule?.let { daysLeftText(it.daysLeft) } ?: "Set semester date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ProgressRing(totals.taughtFraction, "Taught", MaterialTheme.colorScheme.primary)
            Box(Modifier.size(12.dp))
            ProgressRing(totals.learnedFraction, "Learned", MaterialTheme.colorScheme.tertiary)
        }
    }
}

fun daysLeftText(days: Int): String = when (days) {
    0 -> "No teaching days left"
    1 -> "1 teaching day left"
    else -> "$days teaching days left"
}
