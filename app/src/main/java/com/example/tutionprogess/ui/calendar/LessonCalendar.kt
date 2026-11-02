package com.example.tutionprogess.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutionprogess.domain.CalendarEvent
import com.example.tutionprogess.domain.LessonEventKind
import com.example.tutionprogess.ui.components.AccentBarRow
import com.example.tutionprogess.ui.components.DateFormats
import com.example.tutionprogess.ui.components.LocalDatePickerDialog
import com.example.tutionprogess.ui.components.tintedCardColor
import com.example.tutionprogess.ui.theme.calendarColors
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Which marks the calendar shows. */
enum class EventKindFilter(val label: String) {
    TAUGHT("Taught"),
    LEARNED("Learned"),
    BOTH("Both"),
    ;

    fun includes(kind: LessonEventKind): Boolean = when (this) {
        BOTH -> true
        TAUGHT -> kind == LessonEventKind.TAUGHT
        LEARNED -> kind == LessonEventKind.LEARNED
    }
}

@Composable
fun KindFilterChips(selected: EventKindFilter, onSelect: (EventKindFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EventKindFilter.entries.forEach { filter ->
            FilterChip(selected = selected == filter, onClick = { onSelect(filter) }, label = { Text(filter.label) })
        }
    }
}

// The pager covers 200 years of months; page 0 is January 2000.
private val BaseMonth: YearMonth = YearMonth.of(2000, 1)
private const val PAGE_COUNT = 12 * 200

private fun YearMonth.toPage(): Int =
    ((year - BaseMonth.year) * 12 + (monthValue - 1)).coerceIn(0, PAGE_COUNT - 1)

private fun pageToMonth(page: Int): YearMonth = BaseMonth.plusMonths(page.toLong())

/**
 * Month calendar of taught/learned marks with a list of the selected day's events underneath.
 * Swipe or use the arrows to change month. [showStudent] adds student initials/names (Home view).
 */
@Composable
fun LessonCalendar(
    events: List<CalendarEvent>,
    showStudent: Boolean,
    onOpenEvent: (CalendarEvent) -> Unit,
    onChangeDate: (CalendarEvent, LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    val todayPage = YearMonth.from(today).toPage()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = todayPage) { PAGE_COUNT }
    var selectedDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val selected = LocalDate.ofEpochDay(selectedDay)
    var editing by remember { mutableStateOf<CalendarEvent?>(null) }
    val byDate = remember(events) { events.groupBy { it.date } }
    val month = pageToMonth(pagerState.currentPage)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = tintedCardColor()),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
                Row(Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(DateFormats.monthYear(month), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = {
                        selectedDay = today.toEpochDay()
                        scope.launch { pagerState.animateScrollToPage(todayPage) }
                    }) { Text("Today") }
                    IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }
                Legend(Modifier.padding(start = 8.dp, bottom = 12.dp))
                WeekdayHeader()
                HorizontalPager(state = pagerState, verticalAlignment = Alignment.Top, key = { it }) { page ->
                    MonthGrid(
                        month = pageToMonth(page),
                        today = today,
                        selected = selected,
                        byDate = byDate,
                        showStudent = showStudent,
                        onSelect = { selectedDay = it.toEpochDay() },
                    )
                }
            }
        }
        DayEvents(
            date = selected,
            events = byDate[selected].orEmpty(),
            showStudent = showStudent,
            onOpen = onOpenEvent,
            onEdit = { editing = it },
        )
    }

    editing?.let { event ->
        LocalDatePickerDialog(
            initial = event.date,
            minDate = minOf(event.trackingStart, today),
            maxDate = today,
            onDismiss = { editing = null },
            onConfirm = { date ->
                onChangeDate(event, date)
                editing = null
            },
        )
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.calendarColors
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem("Taught", colors.taught)
        LegendItem("Learned", colors.learned)
        Text("Faded = optional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        DayOfWeek.entries.forEach { day ->
            Text(
                day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (day == DayOfWeek.SUNDAY) 0.5f else 1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    byDate: Map<LocalDate, List<CalendarEvent>>,
    showStudent: Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    val offset = month.atDay(1).dayOfWeek.value - 1 // Monday-first
    val days = month.lengthOfMonth()
    val rows = (offset + days + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (col in 0 until 7) {
                    val day = row * 7 + col - offset + 1
                    if (day in 1..days) {
                        val date = month.atDay(day)
                        DayCell(
                            date = date,
                            isToday = date == today,
                            isSelected = date == selected,
                            events = byDate[date].orEmpty(),
                            showStudent = showStudent,
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private const val MAX_TAGS = 2

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    events: List<CalendarEvent>,
    showStudent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    Column(
        modifier
            .height(78.dp)
            .clip(shape)
            .background(
                when {
                    isSelected -> scheme.primary.copy(alpha = 0.12f)
                    isSunday -> scheme.onSurface.copy(alpha = 0.04f)
                    else -> Color.Transparent
                },
            )
            .then(if (isSelected) Modifier.border(1.dp, scheme.primary, shape) else Modifier)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${DateFormats.full(date)}, ${events.size} event${if (events.size == 1) "" else "s"}"
                this.selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .padding(top = 4.dp, bottom = 2.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isToday) scheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    isToday -> scheme.onPrimary
                    isSunday -> scheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else -> scheme.onSurface
                },
            )
        }
        events.take(MAX_TAGS).forEach { EventTag(it, showStudent) }
        if (events.size > MAX_TAGS) {
            Text(
                "+${events.size - MAX_TAGS}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/** Compact tag: "AD Eng 3" on Home, "Eng 3" on a student's calendar. */
fun CalendarEvent.tagLabel(showStudent: Boolean): String =
    if (showStudent) "$studentInitials $subjectShort $lessonNumber" else "$subjectShort $lessonNumber"

@Composable
private fun EventTag(event: CalendarEvent, showStudent: Boolean) {
    val colors = MaterialTheme.calendarColors
    val taught = event.kind == LessonEventKind.TAUGHT
    Text(
        event.tagLabel(showStudent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .alpha(if (event.isOptional) 0.5f else 1f)
            .clip(RoundedCornerShape(4.dp))
            .background(if (taught) colors.taughtContainer else colors.learnedContainer)
            .padding(horizontal = 2.dp),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 12.sp, letterSpacing = 0.sp),
        color = if (taught) colors.onTaughtContainer else colors.onLearnedContainer,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun DayEvents(
    date: LocalDate,
    events: List<CalendarEvent>,
    showStudent: Boolean,
    onOpen: (CalendarEvent) -> Unit,
    onEdit: (CalendarEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            DateFormats.weekday(date),
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.titleSmall,
        )
        if (events.isEmpty()) {
            Text(
                if (date.dayOfWeek == DayOfWeek.SUNDAY) "Sunday - no teaching day." else "Nothing taught or learned on this day.",
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        events.forEach { event ->
            EventRow(event, showStudent, onClick = { onOpen(event) }, onEdit = { onEdit(event) })
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, showStudent: Boolean, onClick: () -> Unit, onEdit: () -> Unit) {
    val colors = MaterialTheme.calendarColors
    val taught = event.kind == LessonEventKind.TAUGHT
    val accent = if (taught) colors.taught else colors.learned
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = tintedCardColor()),
        shape = MaterialTheme.shapes.medium,
    ) {
        AccentBarRow(accent) {
            Row(
                Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp)
                    .alpha(if (event.isOptional) 0.6f else 1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (showStudent) {
                        Text(
                            event.studentName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(event.subjectName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        event.lessonLabel + if (event.isOptional) " (optional)" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(shape = CircleShape, color = accent.copy(alpha = 0.14f), contentColor = accent) {
                    Text(
                        if (taught) "Taught" else "Learned",
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Outlined.EditCalendar,
                        contentDescription = "Change ${if (taught) "taught" else "learned"} date of ${event.subjectName} lesson ${event.lessonNumber}",
                    )
                }
            }
        }
    }
}
