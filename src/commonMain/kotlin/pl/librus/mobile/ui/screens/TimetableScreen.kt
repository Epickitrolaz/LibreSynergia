package pl.librus.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.librus.mobile.domain.models.TimetableDay
import pl.librus.mobile.domain.models.TimetableSlot
import pl.librus.mobile.ui.AppLanguage
import pl.librus.mobile.ui.Strings

@Composable
fun TimetableScreen(
    timetable: List<TimetableDay>,
    strings: Strings = Strings(AppLanguage.ENGLISH)
) {
    var selectedDayIndex by remember {
        mutableStateOf(
            timetable.indexOfFirst {
                it.dayOfWeekName.contains("Dzisiaj") || it.dayOfWeekName.contains("Today")
            }.takeIf { it >= 0 } ?: 0
        )
    }
    var showOnlyChanges by remember { mutableStateOf(false) }

    val activeDay = timetable.getOrNull(selectedDayIndex)
    val lessons = remember(activeDay, showOnlyChanges) {
        val list = activeDay?.lessons ?: emptyList()
        if (showOnlyChanges) list.filter { it.isSubstitution || it.isCancelled } else list
    }

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

        // Days Scrollable Bar
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(timetable) { index, day ->
                val isSelected = index == selectedDayIndex
                val cleanDayName = day.dayOfWeekName.split(" ").first()

                Surface(
                    onClick = { selectedDayIndex = index },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(42.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = cleanDayName,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lessons list
        if (lessons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showOnlyChanges) strings.noChangesMessage else strings.noLessonsDay,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(lessons) { slot ->
                    LessonCard(slot = slot, strings = strings)
                }
            }
        }
    }
}

@Composable
private fun LessonCard(
    slot: TimetableSlot,
    strings: Strings
) {
    val cardColor = when {
        slot.isSubstitution -> Color(0xFFFFF8E1) // Warm amber tint
        slot.isCancelled -> Color(0xFFFFEBEE) // Soft red tint
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Period Number & Hours
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(62.dp)
            ) {
                Text(
                    text = "${slot.lessonNo}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (slot.hourFrom.isNotEmpty()) {
                    Text(
                        text = slot.hourFrom,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = slot.hourTo,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .height(44.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Subject & Details
            Column(modifier = Modifier.weight(1f)) {
                if (slot.isCancelled) {
                    Text(
                        text = slot.subjectName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textDecoration = TextDecoration.LineThrough,
                        color = Color(0xFFC62828)
                    )
                    Text(
                        text = strings.lessonCancelled,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                } else if (slot.isSubstitution) {
                    Text(
                        text = slot.subjectName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFFE65100)
                    )
                    if (slot.orgSubjectName != null) {
                        Text(
                            text = "${strings.replaces}${slot.orgSubjectName}",
                            fontSize = 12.sp,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!slot.substitutionNote.isNullOrEmpty()) {
                        Text(
                            text = slot.substitutionNote,
                            fontSize = 11.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                } else {
                    Text(
                        text = slot.subjectName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (slot.teacherName.isNotEmpty()) {
                        Text(
                            text = slot.teacherName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (slot.classroom.isNotEmpty()) {
                        Text(
                            text = " • ${slot.classroom}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
