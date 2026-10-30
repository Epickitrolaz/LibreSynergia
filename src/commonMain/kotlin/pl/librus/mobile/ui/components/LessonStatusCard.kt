package pl.librus.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*
import pl.librus.mobile.domain.calculations.LessonStatus
import pl.librus.mobile.domain.calculations.LessonStatusCalculator
import pl.librus.mobile.domain.models.TimetableSlot
import pl.librus.mobile.ui.Strings

@Composable
fun LessonStatusCard(
    todayLessons: List<TimetableSlot>,
    strings: Strings,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    var currentTime by remember {
        mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Warsaw")).time)
    }

    // Refresh time ticker every 15 seconds
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Warsaw")).time
            kotlinx.coroutines.delay(15_000)
        }
    }

    val status = remember(todayLessons, currentTime) {
        LessonStatusCalculator.calculateStatus(todayLessons, currentTime)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                is LessonStatus.LessonOngoing -> MaterialTheme.colorScheme.primaryContainer
                is LessonStatus.BreakOngoing -> if (status.isNextCancelled) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.secondaryContainer
                is LessonStatus.BeforeSchool -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            when (status) {
                is LessonStatus.LessonOngoing -> {
                    val lesson = status.lesson
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "${strings.statusLessonNo} ${lesson.lessonNo}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.statusLessonOngoing,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = "${status.minutesToEnd} min ${strings.statusToEnd}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = lesson.subjectName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    val roomText = if (lesson.classroom.isNotEmpty()) " • ${strings.statusRoom} ${lesson.classroom}" else ""
                    Text(
                        text = "${lesson.timeRange}$roomText",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { status.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    )

                    status.nextLesson?.let { next ->
                        Spacer(modifier = Modifier.height(6.dp))
                        val nextRoom = if (next.classroom.isNotEmpty()) " (${strings.statusRoom} ${next.classroom})" else ""
                        Text(
                            text = "${strings.statusNextLesson} ${next.subjectName}$nextRoom",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }

                is LessonStatus.BreakOngoing -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (status.isNextCancelled) Color(0xFFC62828) else MaterialTheme.colorScheme.secondary
                            ) {
                                Text(
                                    text = strings.statusBreakOngoing,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "${status.minutesToNextLesson} min",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (status.isNextCancelled) Color(0xFFC62828) else MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (status.isNextCancelled) {
                        val cancelledRoom = if (status.nextLesson.classroom.isNotEmpty()) " (${strings.statusRoom} ${status.nextLesson.classroom})" else ""
                        Text(
                            text = "${strings.statusCancelled}: ${status.nextLesson.subjectName}$cancelledRoom (${strings.statusLessonNo} ${status.nextLesson.lessonNo})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                        status.nextEffectiveLesson?.let { eff ->
                            val effRoom = if (eff.classroom.isNotEmpty()) " (${strings.statusRoom} ${eff.classroom})" else ""
                            Text(
                                text = "${strings.statusNextEffective} ${eff.subjectName}$effRoom (${strings.statusLessonNo} ${eff.lessonNo})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    } else {
                        val next = status.nextLesson
                        val nextRoom = if (next.classroom.isNotEmpty()) " • ${strings.statusRoom} ${next.classroom}" else ""
                        Text(
                            text = "${strings.statusNextLesson} ${next.subjectName} (${strings.statusLessonNo} ${next.lessonNo})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${next.timeRange}$nextRoom",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                is LessonStatus.BeforeSchool -> {
                    val first = status.firstLesson
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.statusBeforeSchool,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "${status.minutesToStart} min ${strings.statusToStart}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val room = if (first.classroom.isNotEmpty()) " • ${strings.statusRoom} ${first.classroom}" else ""
                    Text(
                        text = "${first.subjectName} (${strings.statusLessonNo} ${first.lessonNo})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "${first.timeRange}$room",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }

                is LessonStatus.SchoolFinished -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.statusSchoolFinished,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is LessonStatus.NoLessonsToday -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.statusNoLessonsToday,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
