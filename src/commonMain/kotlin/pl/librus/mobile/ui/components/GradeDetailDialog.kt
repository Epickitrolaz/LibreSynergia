package pl.librus.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pl.librus.mobile.domain.models.Grade
import pl.librus.mobile.ui.AppLanguage
import pl.librus.mobile.ui.Strings

@Composable
fun GradeDetailDialog(
    grade: Grade,
    onDismiss: () -> Unit,
    strings: Strings = Strings(AppLanguage.ENGLISH)
) {
    val gradeColor = remember(grade.grade, grade.colorHex) {
        getGradeColor(grade.grade, grade.colorHex)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header: Subject & Grade Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = grade.subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = grade.categoryName.ifEmpty { strings.regularGrade },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(gradeColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = grade.grade,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = gradeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Detail Rows
                DetailRow(
                    icon = Icons.Default.Scale,
                    label = strings.gradeWeight,
                    value = "${grade.weight.toInt()}" + if (grade.countToAverage) strings.countsToAverage else strings.doesNotCountToAverage
                )

                if (grade.date.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(
                        icon = Icons.Default.CalendarToday,
                        label = strings.entryDate,
                        value = grade.date
                    )
                }

                if (grade.teacherName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(
                        icon = Icons.Default.Person,
                        label = strings.teacher,
                        value = grade.teacherName
                    )
                }

                if (!grade.comment.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(
                        icon = Icons.AutoMirrored.Filled.Comment,
                        label = strings.teacherComment,
                        value = grade.comment
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.close)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
