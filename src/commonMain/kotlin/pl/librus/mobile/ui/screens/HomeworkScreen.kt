package pl.librus.mobile.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import pl.librus.mobile.domain.models.Homework
import pl.librus.mobile.ui.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkScreen(
    homeworks: List<Homework>,
    strings: Strings,
    onToggleDone: (String) -> Unit,
    onBack: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf(0) } // 0: To Do, 1: Done, 2: All

    val filteredHomeworks = remember(homeworks, selectedFilter) {
        when (selectedFilter) {
            0 -> homeworks.filter { !it.isDone }
            1 -> homeworks.filter { it.isDone }
            else -> homeworks
        }
    }

    val pendingCount = remember(homeworks) { homeworks.count { !it.isDone } }
    val completedCount = remember(homeworks) { homeworks.count { it.isDone } }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.close
                    )
                }
                Text(
                    text = strings.homeworkTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (pendingCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "$pendingCount ${strings.homeworkPending.lowercase()}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

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
                label = { Text("${strings.homeworkPending} ($pendingCount)") }
            )
            FilterChip(
                selected = selectedFilter == 1,
                onClick = { selectedFilter = 1 },
                label = { Text("${strings.homeworkCompleted} ($completedCount)") }
            )
            FilterChip(
                selected = selectedFilter == 2,
                onClick = { selectedFilter = 2 },
                label = { Text("${strings.homeworkAll} (${homeworks.size})") }
            )
        }

        // List with pull-to-refresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (filteredHomeworks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.noHomework,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredHomeworks, key = { it.id }) { homework ->
                        HomeworkCard(
                            homework = homework,
                            strings = strings,
                            onToggleDone = { onToggleDone(homework.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeworkCard(
    homework: Homework,
    strings: Strings,
    onToggleDone: () -> Unit
) {
    val today = remember { Clock.System.todayIn(TimeZone.of("Europe/Warsaw")) }
    val (dueLabel, isOverdue) = remember(homework.date, today) {
        try {
            val due = LocalDate.parse(homework.date)
            when {
                due == today -> strings.homeworkDueToday to false
                due.toEpochDays() == today.toEpochDays() + 1 -> strings.homeworkDueTomorrow to false
                due < today -> strings.homeworkOverdue to true
                else -> homework.date to false
            }
        } catch (_: Exception) {
            homework.date to false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleDone),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (homework.isDone) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (homework.isDone) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox / Toggle Button
            IconButton(
                onClick = onToggleDone,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (homework.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (homework.isDone) strings.homeworkCompleted else strings.homeworkPending,
                    tint = if (homework.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Subject & Due Date Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = homework.subjectName.ifEmpty { "Zadanie" },
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            homework.isDone -> MaterialTheme.colorScheme.surfaceVariant
                            isOverdue -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = when {
                                    homework.isDone -> MaterialTheme.colorScheme.onSurfaceVariant
                                    isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${strings.homeworkDue}$dueLabel",
                                color = when {
                                    homework.isDone -> MaterialTheme.colorScheme.onSurfaceVariant
                                    isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Content
                Text(
                    text = homework.content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    textDecoration = if (homework.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (homework.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Teacher and Add Date footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (homework.teacherName.isNotBlank()) {
                        Text(
                            text = homework.teacherName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (homework.attachments.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${homework.attachments.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
