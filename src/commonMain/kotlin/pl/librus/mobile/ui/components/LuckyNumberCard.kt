package pl.librus.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.librus.mobile.domain.models.LuckyNumber
import pl.librus.mobile.domain.models.StudentProfile
import pl.librus.mobile.ui.AppLanguage
import pl.librus.mobile.ui.Strings
import pl.librus.mobile.ui.icons.LibrusIcons

@Composable
fun LuckyNumberCard(
    luckyNumber: LuckyNumber?,
    profile: StudentProfile?,
    modifier: Modifier = Modifier,
    strings: Strings = Strings(AppLanguage.ENGLISH)
) {
    if (luckyNumber == null) return

    val isLucky = profile?.classRegisterNumber == luckyNumber.number

    val containerColor = if (isLucky) {
        Color(0xFFFFF8E1) // Soft Gold
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isLucky) {
        Color(0xFFE65100)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isLucky) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLucky) LibrusIcons.Celebration else Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = if (isLucky) strings.luckyNumberWon else strings.luckyNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = contentColor
                    )
                    Text(
                        text = if (isLucky) {
                            strings.luckyNumberWonDesc
                        } else {
                            "${strings.yourRegisterNo}${profile?.classRegisterNumber ?: "-"}"
                        },
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLucky) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${luckyNumber.number}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        }
    }
}
