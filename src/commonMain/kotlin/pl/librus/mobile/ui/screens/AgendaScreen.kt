package pl.librus.mobile.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import pl.librus.mobile.domain.models.AgendaEvent
import pl.librus.mobile.domain.models.AgendaEventType
import pl.librus.mobile.ui.Strings
import pl.librus.mobile.ui.components.parseColorHex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    events: List<AgendaEvent>,
    strings: Strings,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf(0) } // 0: Upcoming, 1: Exams only, 2: All
    val today = remember { Clock.System.todayIn(TimeZone.of("Europe/Warsaw")) }

    val filteredEvents = remember(events, selectedFilter, today) {
        val sorted = events.sortedWith(compareBy<AgendaEvent> { it.date }.thenBy { it.lessonNo ?: 99 })
        when (selectedFilter) {
            0 -> sorted.filter {
                try {
                    LocalDate.parse(it.date) >= today
                } catch (_: Exception) {
                    true
                }
            }
            1 -> sorted.filter { it.type == AgendaEventType.EXAM || it.type == AgendaEventType.QUIZ }
            else -> sorted
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == 0,
                onClick = { selectedFilter = 0 },
                label = { Text(strings.agendaUpcoming) }
            )
            FilterChip(
                selected = selectedFilter == 1,
                onClick = { selectedFilter = 1 },
                label = { Text(strings.agendaExams) }
            )
            FilterChip(
                selected = selectedFilter == 2,
                onClick = { selectedFilter = 2 },
                label = { Text("${strings.agendaAll} (${events.size})") }
            )
        }

        // Continuous list with dates next to events
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.noAgendaEvents,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEvents, key = { it.id }) { event ->
                        AgendaEventRow(
                            event = event,
                            today = today,
                            strings = strings
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgendaEventRow(
    event: AgendaEvent,
    today: LocalDate,
    strings: Strings
) {
    val eventDate = remember(event.date) {
        try {
            LocalDate.parse(event.date)
        } catch (_: Exception) {
            null
        }
    }

    val isToday = eventDate == today
    val isPast = eventDate != null && eventDate < today

    val dayOfWeekAbbr = remember(eventDate, strings) {
        when (eventDate?.dayOfWeek?.name) {
            "MONDAY" -> strings.dayPn
            "TUESDAY" -> strings.dayWt
            "WEDNESDAY" -> strings.daySr
            "THURSDAY" -> strings.dayCz
            "FRIDAY" -> strings.dayPt
            "SATURDAY" -> strings.daySo
            "SUNDAY" -> strings.dayNd
            else -> ""
        }
    }

    val dayOfMonth = eventDate?.dayOfMonth?.toString() ?: ""
    val monthShort = remember(eventDate) {
        when (eventDate?.monthNumber) {
            1 -> "sty"
            2 -> "lut"
            3 -> "mar"
            4 -> "kwi"
            5 -> "maj"
            6 -> "cze"
            7 -> "lip"
            8 -> "sie"
            9 -> "wrz"
            10 -> "paź"
            11 -> "lis"
            12 -> "gru"
            else -> ""
        }
    }

    val badgeColor = remember(event.colorHex) {
        try {
            Color(parseColorHex(event.colorHex))
        } catch (_: Throwable) {
            when (event.type) {
                AgendaEventType.EXAM -> Color(0xFFF44336)
                AgendaEventType.QUIZ -> Color(0xFFFF9800)
                AgendaEventType.PROJECT -> Color(0xFF9C27B0)
                AgendaEventType.TRIP -> Color(0xFF4CAF50)
                AgendaEventType.MEETING -> Color(0xFF2196F3)
                AgendaEventType.HOMEWORK -> Color(0xFF3F51B5)
                else -> Color(0xFF607D8B)
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Continuous Date indicator on the left side
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(52.dp)
                .padding(top = 4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when {
                    isToday -> MaterialTheme.colorScheme.primary
                    isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text(
                        text = dayOfWeekAbbr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isToday -> MaterialTheme.colorScheme.onPrimary
                            isPast -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                    Text(
                        text = dayOfMonth,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isToday -> MaterialTheme.colorScheme.onPrimary
                            isPast -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                    Text(
                        text = monthShort,
                        fontSize = 10.sp,
                        color = when {
                            isToday -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            else -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Event Card on the right
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isPast) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isPast) 0.dp else 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Category & Subject Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = event.typeName.ifEmpty { "Wydarzenie" }.uppercase(),
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (event.subjectName.isNotBlank()) {
                        Text(
                            text = event.subjectName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Event Title
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )

                // Time / Lesson period
                if (event.lessonNo != null || event.timeFrom != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val lessonLabel = if (event.lessonNo != null) "Lekcja ${event.lessonNo}" else ""
                        val timeLabel = if (event.timeFrom != null && event.timeTo != null) {
                            "${event.timeFrom} - ${event.timeTo}"
                        } else event.timeFrom.orEmpty()
                        val combined = listOf(lessonLabel, timeLabel).filter { it.isNotBlank() }.joinToString(" • ")
                        Text(
                            text = combined,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Description
                if (event.description.isNotBlank() && event.description != event.title) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }

                // Teacher
                if (event.teacherName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = event.teacherName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
