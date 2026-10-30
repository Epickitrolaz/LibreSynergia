package pl.librus.mobile.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*
import pl.librus.mobile.domain.calculations.LessonStatusCalculator
import pl.librus.mobile.domain.models.TimetableDay
import pl.librus.mobile.domain.models.TimetableSlot
import pl.librus.mobile.ui.AppLanguage
import pl.librus.mobile.ui.Strings
import pl.librus.mobile.ui.animation.LocalMotionSpec
import pl.librus.mobile.ui.animation.MotionSpec
import pl.librus.mobile.ui.components.LessonStatusCard
import pl.librus.mobile.ui.icons.LibrusIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    timetable: List<TimetableDay>,
    strings: Strings = Strings(AppLanguage.ENGLISH),
    isLoading: Boolean = false,
    syncFailed: Boolean = false,
    onRetry: () -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val today = Clock.System.todayIn(TimeZone.of("Europe/Warsaw"))
    val calendarDays = remember(timetable, today) {
        timetable.ifEmpty {
            val monday = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
            (0..6).map { offset ->
                val date = monday.plus(offset, DateTimeUnit.DAY)
                TimetableDay(date.toString(), date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }

    var selectedDayIndex by remember(calendarDays.map { it.date }, today) {
        mutableStateOf(
            calendarDays.indexOfFirst {
                it.date == today.toString()
            }.takeIf { it >= 0 } ?: 0
        )
    }
    var showOnlyChanges by remember { mutableStateOf(false) }
    val motionSpec = LocalMotionSpec.current

    var currentTime by remember {
        mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Warsaw")).time)
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Warsaw")).time
            kotlinx.coroutines.delay(15_000)
        }
    }

    val activeDay = calendarDays.getOrNull(selectedDayIndex)
    val isViewingToday = activeDay?.date == today.toString()

    val lessons = remember(activeDay, showOnlyChanges) {
        val list = activeDay?.lessons ?: emptyList()
        if (showOnlyChanges) list.filter { it.isSubstitution || it.isCancelled } else list
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.timetableTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Changes Filter Chip
                FilterChip(
                    selected = showOnlyChanges,
                    onClick = { showOnlyChanges = !showOnlyChanges },
                    label = { Text(strings.filterChangesOnly, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Non-scrolling 2-letter Days Bar (fits all 5 school days without horizontal scrolling - Issue 9)
            val displayDays = remember(calendarDays) {
                calendarDays.take(5) // Pn, Wt, Śr, Cz, Pt
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                displayDays.forEachIndexed { index, day ->
                    val isSelected = index == selectedDayIndex
                    val isToday = day.date == today.toString()
                    val twoLetterLabel = getTwoLetterDay(day.dayOfWeekName, strings)
                    val dayOfMonth = day.date.split("-").lastOrNull()?.toIntOrNull()?.toString() ?: ""

                    Surface(
                        onClick = { selectedDayIndex = index },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = twoLetterLabel,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (dayOfMonth.isNotEmpty()) {
                                Text(
                                    text = dayOfMonth,
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    } else if (isToday) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedContent(
                targetState = selectedDayIndex,
                transitionSpec = {
                    if (!motionSpec.isAnimationsEnabled) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else if (targetState > initialState) {
                        (slideInHorizontally(motionSpec.tween(280, easing = MotionSpec.EmphasizedDecelerate)) { it / 3 } + fadeIn(motionSpec.tween(200)))
                            .togetherWith(slideOutHorizontally(motionSpec.tween(280, easing = MotionSpec.EmphasizedAccelerate)) { -it / 3 } + fadeOut(motionSpec.tween(180)))
                    } else {
                        (slideInHorizontally(motionSpec.tween(280, easing = MotionSpec.EmphasizedDecelerate)) { -it / 3 } + fadeIn(motionSpec.tween(200)))
                            .togetherWith(slideOutHorizontally(motionSpec.tween(280, easing = MotionSpec.EmphasizedAccelerate)) { it / 3 } + fadeOut(motionSpec.tween(180)))
                    }
                },
                modifier = Modifier.fillMaxSize(),
                label = "timetableDayAnim"
            ) { targetDayIdx ->
                val currentDay = calendarDays.getOrNull(targetDayIdx)
                val dayViewingToday = currentDay?.date == today.toString()
                val dayLessons = if (showOnlyChanges) {
                    currentDay?.lessons?.filter { it.isSubstitution || it.isCancelled } ?: emptyList()
                } else {
                    currentDay?.lessons ?: emptyList()
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Szkolny.eu-style Lesson Status Card when viewing today (Issue 10)
                    if (dayViewingToday) {
                        currentDay?.let { day ->
                            LessonStatusCard(
                                todayLessons = day.lessons,
                                strings = strings
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(strings.timetableLoading, modifier = Modifier.padding(vertical = 8.dp))
                    } else if (syncFailed) {
                        Text(
                            strings.timetableUnavailable,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                        TextButton(onClick = onRetry) { Text(strings.retryTimetable) }
                    }

                    // Lessons list
                    if (dayLessons.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (showOnlyChanges && timetable.isNotEmpty()) {
                                    Icon(
                                        imageVector = LibrusIcons.ThumbUp,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                                Text(
                                    text = when {
                                        timetable.isEmpty() && isLoading -> strings.timetableLoading
                                        timetable.isEmpty() -> strings.timetableNotLoaded
                                        showOnlyChanges -> strings.noChangesMessage
                                        else -> strings.noLessonsDay
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        val nowMinutes = currentTime.hour * 60 + currentTime.minute
                        val timeFormatted = "${currentTime.hour.toString().padStart(2, '0')}:${currentTime.minute.toString().padStart(2, '0')}"

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // If viewing today, check if current time is before the first lesson
                            if (dayViewingToday && dayLessons.isNotEmpty()) {
                                val firstStart = LessonStatusCalculator.parseTimeToMinutes(dayLessons.first().hourFrom) ?: 0
                                if (nowMinutes < firstStart) {
                                    item(key = "current_time_bar_top") {
                                        CurrentTimeIndicatorBar(timeStr = timeFormatted, strings = strings)
                                    }
                                }
                            }

                            itemsIndexed(dayLessons, key = { _, slot -> slot.id }) { index, slot ->
                                val slotStart = LessonStatusCalculator.parseTimeToMinutes(slot.hourFrom) ?: 0
                                val slotEnd = LessonStatusCalculator.parseTimeToMinutes(slot.hourTo) ?: (slotStart + 45)

                                val isCurrentLesson = dayViewingToday && (nowMinutes in slotStart until slotEnd)

                                LessonCard(slot = slot, strings = strings, isCurrent = isCurrentLesson)

                                // If viewing today, check if current time is inside or right after this lesson (break)
                                if (dayViewingToday) {
                                    val nextSlot = dayLessons.getOrNull(index + 1)
                                    if (nextSlot != null) {
                                        val nextStart = LessonStatusCalculator.parseTimeToMinutes(nextSlot.hourFrom) ?: (slotEnd + 10)
                                        if (nowMinutes in slotEnd until nextStart) {
                                            CurrentTimeIndicatorBar(timeStr = timeFormatted, strings = strings)
                                        }
                                    } else {
                                        // After last lesson
                                        if (nowMinutes >= slotEnd) {
                                            CurrentTimeIndicatorBar(timeStr = timeFormatted, strings = strings)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getTwoLetterDay(dayName: String, strings: Strings): String {
    val lower = dayName.lowercase()
    return when {
        lower.contains("pon") || lower.contains("mon") -> strings.dayPn
        lower.contains("wto") || lower.contains("tue") -> strings.dayWt
        lower.contains("śro") || lower.contains("sro") || lower.contains("wed") -> strings.daySr
        lower.contains("czw") || lower.contains("thu") -> strings.dayCz
        lower.contains("pią") || lower.contains("pia") || lower.contains("fri") -> strings.dayPt
        lower.contains("sob") || lower.contains("sat") -> strings.daySo
        lower.contains("nie") || lower.contains("sun") -> strings.dayNd
        else -> dayName.take(2).uppercase()
    }
}

@Composable
private fun CurrentTimeIndicatorBar(timeStr: String, strings: Strings) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(10.dp)
        ) {}
        HorizontalDivider(
            color = MaterialTheme.colorScheme.primary,
            thickness = 2.dp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 2.dp)
        ) {
            Text(
                text = "${strings.currentTimeLabel} $timeStr",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun LessonCard(
    slot: TimetableSlot,
    strings: Strings,
    isCurrent: Boolean = false
) {
    val cardColor = when {
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        slot.isSubstitution -> Color(0xFFFFF8E1)
        slot.isCancelled -> Color(0xFFFFEBEE)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 3.dp else 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${slot.lessonNo}.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(26.dp)
                    )

                    Text(
                        text = slot.subjectName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textDecoration = if (slot.isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (slot.isCancelled) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isCurrent) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = strings.currentTimeLabel,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (slot.isSubstitution) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE65100)
                    ) {
                        Text(
                            text = strings.badgeSubstitution,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (slot.isCancelled) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFC62828)
                    ) {
                        Text(
                            text = strings.badgeCancelled,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${slot.timeRange}${if (slot.classroom.isNotEmpty()) " • ${strings.statusRoom} ${slot.classroom}" else ""}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (slot.teacherName.isNotEmpty()) {
                    Text(
                        text = slot.teacherName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (slot.isSubstitution && slot.orgSubjectName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${strings.replaces}${slot.orgSubjectName}",
                    fontSize = 12.sp,
                    color = Color(0xFFE65100),
                    modifier = Modifier.padding(start = 26.dp)
                )
            }
        }
    }
}
