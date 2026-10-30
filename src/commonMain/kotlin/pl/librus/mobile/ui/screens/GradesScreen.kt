package pl.librus.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.librus.mobile.domain.models.Grade
import pl.librus.mobile.domain.models.SubjectGradeGroup
import pl.librus.mobile.ui.AppLanguage
import pl.librus.mobile.ui.Strings
import pl.librus.mobile.ui.components.GradeBadge
import pl.librus.mobile.ui.components.getGradeColor
import pl.librus.mobile.ui.components.GradeDetailDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GradesScreen(
    groups: List<SubjectGradeGroup>,
    strings: Strings = Strings(AppLanguage.ENGLISH),
    onGradeClick: (Grade) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var selectedSemesterTab by remember { mutableStateOf(0) } // 0: Semestr 1, 1: Semestr 2, 2: Cały rok

    // Filter out subjects with no grades for the selected semester/year (Issue 8)
    val visibleGroups = remember(groups, selectedSemesterTab) {
        groups.filter { group ->
            when (selectedSemesterTab) {
                0 -> group.semester1Grades.isNotEmpty() || group.semester1Proposal != null || group.semester1Final != null
                1 -> group.semester2Grades.isNotEmpty() || group.semester2Proposal != null || group.finalGrade != null
                else -> group.allGrades.isNotEmpty()
            }
        }
    }

    val overallGpa = remember(visibleGroups, selectedSemesterTab) {
        val avgs = visibleGroups.mapNotNull { group ->
            when (selectedSemesterTab) {
                0 -> group.semester1Average
                1 -> group.semester2Average
                else -> group.yearAverage ?: group.semester1Average
            }
        }
        if (avgs.isEmpty()) null else (avgs.average() * 100.0).toInt() / 100.0
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Screen Title & Overall Average Banner
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = strings.gradesTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = when (selectedSemesterTab) {
                                    0 -> strings.gpaSem1
                                    1 -> strings.gpaSem2
                                    else -> strings.gpaAnnual
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Text(
                                text = overallGpa?.let { "$it" } ?: "-",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Semester Switcher Tabs
                TabRow(
                    selectedTabIndex = selectedSemesterTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedSemesterTab == 0,
                        onClick = { selectedSemesterTab = 0 },
                        text = { Text(strings.tabSem1, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedSemesterTab == 1,
                        onClick = { selectedSemesterTab = 1 },
                        text = { Text(strings.tabSem2, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedSemesterTab == 2,
                        onClick = { selectedSemesterTab = 2 },
                        text = { Text(strings.tabYear, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Subject Cards List (Only subjects that have grades)
                if (visibleGroups.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noGradesInSemester,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(visibleGroups) { group ->
                            val displayedGrades = when (selectedSemesterTab) {
                                0 -> group.semester1Grades
                                1 -> group.semester2Grades
                                else -> group.semester1Grades + group.semester2Grades
                            }

                            val subjectAvg = when (selectedSemesterTab) {
                                0 -> group.semester1Average
                                1 -> group.semester2Average
                                else -> group.yearAverage ?: group.semester1Average
                            }

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Subject Row Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = group.subject.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )

                                        val avgColor = if (subjectAvg != null) {
                                            getGradeColor(subjectAvg.toString())
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (subjectAvg != null) {
                                                avgColor.copy(alpha = 0.15f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ) {
                                            Text(
                                                text = subjectAvg?.let { "${strings.avgPrefix}$it" } ?: strings.noGrades,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = avgColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Grades Chips Flow / Row
                                    if (displayedGrades.isEmpty()) {
                                        Text(
                                            text = strings.noGrades,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    } else {
                                        OptInFlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            displayedGrades.forEach { grade ->
                                                GradeBadge(
                                                    grade = grade,
                                                    onClick = { onGradeClick(grade) }
                                                )
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptInFlowRow(
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    FlowRow(
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}
