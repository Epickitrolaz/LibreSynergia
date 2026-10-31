package pl.librus.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*
import pl.librus.mobile.domain.calculations.GradeCalculator
import pl.librus.mobile.domain.models.*
import pl.librus.mobile.ui.AppLanguage
import pl.librus.mobile.ui.Strings
import pl.librus.mobile.ui.components.*
import pl.librus.mobile.ui.icons.LibrusIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profile: StudentProfile?,
    children: List<ChildAccount>,
    activeChild: ChildAccount?,
    luckyNumber: LuckyNumber?,
    grades: List<SubjectGradeGroup>,
    recentGrades: List<Grade>,
    timetable: List<TimetableDay>,
    attendanceStats: AttendanceStats,
    notices: List<SchoolNotice>,
    strings: Strings = Strings(AppLanguage.ENGLISH),
    onOpenAccountSwitcher: () -> Unit,
    onNavigateToGrades: () -> Unit,
    onNavigateToSimulator: () -> Unit = onNavigateToGrades,
    onNavigateToTimetable: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToNotices: () -> Unit = {},
    onGradeClick: (Grade) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    val overallGpa = remember(grades) { GradeCalculator.calculateOverallGpa(grades) }
    val today = Clock.System.todayIn(TimeZone.of("Europe/Warsaw"))
    val todaySchedule = remember(timetable, today) {
        timetable.find { it.date == today.toString() }
    }
    val unreadNotices = remember(notices) {
        notices.filter { !it.isRead }.sortedWith(
            compareByDescending<SchoolNotice> { it.date }
                .thenByDescending { it.id }
        )
    }

    // Reverse / sort grades so newest/latest grades appear first
    val previewGrades = remember(recentGrades) {
        recentGrades.sortedWith(
            compareByDescending<Grade> { it.date.ifEmpty { it.addDate } }
                .thenByDescending { it.id }
        ).take(5)
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Student Profile Header Card
            StudentHeaderCard(
                profile = profile,
                children = children,
                activeChild = activeChild,
                onOpenAccountSwitcher = onOpenAccountSwitcher,
                strings = strings
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Lesson Status Countdown Card (Szkolny.eu style)
            val todayLessons = todaySchedule?.lessons ?: emptyList()
            LessonStatusCard(
                todayLessons = todayLessons,
                strings = strings,
                onClick = onNavigateToTimetable
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Lucky Number Card
            LuckyNumberCard(
                luckyNumber = luckyNumber,
                profile = profile,
                strings = strings
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Stats Row (GPA & Attendance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onNavigateToSimulator),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.gradeAverage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = overallGpa?.let { "$it" } ?: "-",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (overallGpa != null && overallGpa >= 4.75) {
                                Icon(
                                    imageVector = LibrusIcons.HonorRibbon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (overallGpa != null && overallGpa >= 4.75) strings.honorRoll else strings.allSubjects,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onNavigateToAttendance),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.attendance,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${attendanceStats.presencePercentage}%",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${attendanceStats.presentCount}/${attendanceStats.totalHours} ${strings.hours}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Today's Lessons Preview Card (Pressing anywhere on the tile opens Timetable)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToTimetable),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val localizedDayName = todaySchedule?.date?.let { dateStr ->
                        runCatching { LocalDate.parse(dateStr).dayOfWeek }.getOrNull()
                    }?.let { strings.getDayName(it) } ?: todaySchedule?.dayOfWeekName ?: ""

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.timetableToday}$localizedDayName",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val lessons = todaySchedule?.lessons?.take(4) ?: emptyList()
                    if (lessons.isEmpty()) {
                        Text(
                            text = strings.noLessonsToday,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        lessons.forEach { slot ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${slot.lessonNo}.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = slot.subjectName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    val room = if (slot.classroom.isNotEmpty()) " • ${strings.statusRoom} ${slot.classroom}" else ""
                                    Text(
                                        text = "${slot.timeRange}$room",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (slot.isSubstitution) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFF3E0)
                                    ) {
                                        Text(
                                            text = strings.badgeSubstitution,
                                            color = Color(0xFFE65100),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else if (slot.isCancelled) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFEBEE)
                                    ) {
                                        Text(
                                            text = strings.badgeCancelled,
                                            color = Color(0xFFC62828),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recent Grades Preview Card (Pressing anywhere on the tile opens Grades)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToGrades),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.recentGrades,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (previewGrades.isEmpty()) {
                        Text(
                            text = strings.noRecentGrades,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        previewGrades.forEach { grade ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onGradeClick(grade) }
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = grade.subjectName,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${grade.categoryName.ifEmpty { strings.regularGrade }} • ${strings.weightPrefix} ${grade.weight.toInt()}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                GradeBadge(grade = grade, onClick = { onGradeClick(grade) })
                            }
                        }
                    }
                }
            }
        }
    }
}
