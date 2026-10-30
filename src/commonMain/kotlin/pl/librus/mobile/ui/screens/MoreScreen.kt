package pl.librus.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.librus.mobile.domain.models.StudentProfile
import pl.librus.mobile.ui.Strings

@Composable
fun MoreScreen(
    profile: StudentProfile?,
    unreadMessagesCount: Int,
    pendingHomeworkCount: Int,
    unreadNoticesCount: Int,
    strings: Strings,
    onNavigateToMessages: () -> Unit,
    onNavigateToHomework: () -> Unit,
    onNavigateToNotices: () -> Unit,
    onNavigateToAgenda: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Screen Title
        Text(
            text = strings.moreTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Section: School & Communication
        Text(
            text = strings.sectionSchoolCommunication,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // Messages
                MoreHubItem(
                    title = strings.messagesTitle,
                    subtitle = strings.messagesDesc,
                    icon = Icons.Default.Email,
                    iconTint = MaterialTheme.colorScheme.primary,
                    badgeCount = unreadMessagesCount,
                    onClick = onNavigateToMessages
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Homework
                MoreHubItem(
                    title = strings.homeworkTitle,
                    subtitle = strings.homeworkDesc,
                    icon = Icons.Default.EditNote,
                    iconTint = Color(0xFFE91E63),
                    badgeCount = pendingHomeworkCount,
                    onClick = onNavigateToHomework
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Notice Board (Single exclusive place for it!)
                MoreHubItem(
                    title = strings.noticeBoardMenu,
                    subtitle = strings.noticeBoardDesc,
                    icon = Icons.Default.Campaign,
                    iconTint = Color(0xFFFF9800),
                    badgeCount = unreadNoticesCount,
                    onClick = onNavigateToNotices
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Agenda
                MoreHubItem(
                    title = strings.agendaTitle,
                    subtitle = strings.agendaDesc,
                    icon = Icons.AutoMirrored.Filled.EventNote,
                    iconTint = Color(0xFF4CAF50),
                    badgeCount = 0,
                    onClick = onNavigateToAgenda
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Application
        Text(
            text = strings.sectionApp,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                MoreHubItem(
                    title = strings.settingsTitle,
                    subtitle = strings.settingsDesc,
                    icon = Icons.Default.Settings,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    badgeCount = 0,
                    onClick = onNavigateToSettings
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MoreHubItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    badgeCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = iconTint.copy(alpha = 0.12f),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        if (badgeCount > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Text(
                    text = "$badgeCount",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
