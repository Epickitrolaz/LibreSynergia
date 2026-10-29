package pl.librus.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import pl.librus.mobile.ui.components.GradeDetailDialog

@Composable
fun GradesScreen(
    groups: List<SubjectGradeGroup>,
    strings: Strings = Strings(AppLanguage.ENGLISH),
    onGradeClick: (Grade) -> Unit
) {
    var selectedSemesterTab by remember { mutableStateOf(0) } // 0: Semestr 1, 1: Semestr 2, 2: Cały rok
    var selectedGradeForDialog by remember { mutableStateOf<Grade?>(null) }

    val overallGpa = remember(groups, selectedSemesterTab) {
        val avgs = groups.mapNotNull { group ->
            when (selectedSemesterTab) {
                0 -> group.semester1Average
                1 -> group.semester2Average
                else -> group.yearAverage ?: group.semester1Average
            }
        }
        if (avgs.isEmpty()) null else (avgs.average() * 100.0).toInt() / 100.0
    }

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

            // Subject Cards List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(groups) { group ->
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

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (subjectAvg != null && subjectAvg >= 4.5) {
                                        Color(0xFFE8F5E9)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = subjectAvg?.let { "${strings.avgPrefix}$it" } ?: strings.noGrades,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (subjectAvg != null && subjectAvg >= 4.5) {
                                            Color(0xFF2E7D32)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Grades Badges Row
                            if (displayedGrades.isEmpty()) {
                                Text(
                                    text = strings.noGradesInSemester,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    displayedGrades.forEach { grade ->
                                        GradeBadge(
                                            grade = grade,
                                            onClick = {
                                                selectedGradeForDialog = grade
                                                onGradeClick(grade)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Grade Detail Dialog
        selectedGradeForDialog?.let { grade ->
            GradeDetailDialog(
                grade = grade,
                onDismiss = { selectedGradeForDialog = null },
                strings = strings
            )
        }
    }
}
