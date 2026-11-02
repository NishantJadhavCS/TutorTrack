package com.example.tutionprogess.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tutionprogess.data.DecodedBackup
import com.example.tutionprogess.data.StudentWithSubjects
import com.example.tutionprogess.data.ThemeMode
import com.example.tutionprogess.ui.AppViewModelProvider
import com.example.tutionprogess.ui.components.BackButton
import com.example.tutionprogess.ui.components.ConfirmDialog
import com.example.tutionprogess.ui.components.tintedCardColor
import com.example.tutionprogess.ui.student.EditStudentSheet
import java.time.LocalDate

private const val NEW_STUDENT = 0L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    // Student id being edited (NEW_STUDENT for "add") / deleted; saved so sheets survive rotation.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::export)
    }
    // Many file managers report .json files as octet-stream or text, so accept any file and validate it.
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::readImport)
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(title = { Text("Settings") }, navigationIcon = { BackButton(onBack) }, scrollBehavior = scrollBehavior)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionTitle("Appearance") }
            item {
                SectionCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Theme", style = MaterialTheme.typography.titleSmall)
                        ThemeSelector(themeMode, viewModel::setThemeMode)
                    }
                }
            }

            item { SectionTitle("Students") }
            item {
                SectionCard {
                    val list = students
                    if (list == null) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    } else {
                        if (list.isEmpty()) {
                            Text(
                                "No students yet.",
                                Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        list.forEach { item ->
                            StudentItem(
                                item = item,
                                onEdit = { editingId = item.student.id },
                                onDelete = { deletingId = item.student.id },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        ActionItem(Icons.Rounded.PersonAdd, "Add student", onClick = { editingId = NEW_STUDENT })
                    }
                }
            }

            item { SectionTitle("Backup") }
            item {
                SectionCard {
                    ActionItem(
                        icon = Icons.Rounded.FileUpload,
                        title = "Export data",
                        subtitle = "Save all students, subjects and lessons to a JSON file",
                        enabled = !busy,
                        onClick = { exportLauncher.launch("tutortrack-backup-${LocalDate.now()}.json") },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ActionItem(
                        icon = Icons.Rounded.FileDownload,
                        title = "Import data",
                        subtitle = "Replace all data with a backup file",
                        enabled = !busy,
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                    )
                }
            }

            item { SectionTitle("About") }
            item {
                SectionCard {
                    val context = LocalContext.current
                    val version = remember { appVersion(context) }
                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                        headlineContent = { Text("App version") },
                        supportingContent = { Text(version) },
                        colors = ListItemDefaults.colors(containerColor = tintedCardColor()),
                    )
                }
            }
        }
    }

    // Sheets and dialogs
    val list = students.orEmpty()
    editingId?.let { id ->
        val existing = list.find { it.student.id == id }?.student
        if (id != NEW_STUDENT && existing == null) {
            LaunchedEffect(Unit) { editingId = null }
        } else {
            EditStudentSheet(
                initial = existing,
                onDismiss = { editingId = null },
                onSave = { viewModel.saveStudent(it); editingId = null },
            )
        }
    }
    deletingId?.let { id ->
        val item = list.find { it.student.id == id }
        if (item == null) {
            LaunchedEffect(Unit) { deletingId = null }
        } else {
            val lessons = item.subjects.sumOf { it.lessons.size }
            ConfirmDialog(
                title = "Delete ${item.student.fullName}?",
                message = "This also deletes ${item.subjects.size} subject(s) and $lessons lesson(s). This can't be undone.",
                confirmText = "Delete",
                onDismiss = { deletingId = null },
                onConfirm = { viewModel.deleteStudent(item.student); deletingId = null },
            )
        }
    }
    pendingImport?.let { backup -> ImportConfirmDialog(backup, viewModel::cancelImport, viewModel::confirmImport) }
}

@Composable
private fun ImportConfirmDialog(backup: DecodedBackup, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val data = backup.data
    val exported = backup.exportedAt?.let { " from ${it.replace('T', ' ')}" }.orEmpty()
    ConfirmDialog(
        title = "Replace all data?",
        message = "The backup$exported has ${data.students.size} student(s), ${data.subjects.size} subject(s) and " +
            "${data.lessons.size} lesson(s).\n\nEverything currently in the app will be replaced. " +
            "Consider exporting a backup first.",
        confirmText = "Replace",
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    )
}

@Composable
private fun ThemeSelector(selected: ThemeMode?, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(label) }
        }
    }
}

@Composable
private fun StudentItem(item: StudentWithSubjects, onEdit: () -> Unit, onDelete: () -> Unit) {
    val student = item.student
    ListItem(
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(student.fullName.trim().take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
                }
            }
        },
        headlineContent = { Text(student.fullName) },
        supportingContent = {
            val subjects = "${item.subjects.size} subject${if (item.subjects.size == 1) "" else "s"}"
            Text(if (student.standard.isBlank()) subjects else "${student.standard} · $subjects")
        },
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Edit ${student.fullName}") }
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete ${student.fullName}") }
            }
        },
        colors = ListItemDefaults.colors(containerColor = tintedCardColor()),
    )
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Surface(onClick = onClick, enabled = enabled, color = tintedCardColor()) {
        ListItem(
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            headlineContent = { Text(title) },
            supportingContent = subtitle?.let { { Text(it) } },
            colors = ListItemDefaults.colors(containerColor = tintedCardColor()),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = tintedCardColor()),
        shape = MaterialTheme.shapes.large,
    ) { Column { content() } }
}

private fun appVersion(context: Context): String = try {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    "${info.versionName} ($code)"
} catch (_: PackageManager.NameNotFoundException) {
    "Unknown"
}
