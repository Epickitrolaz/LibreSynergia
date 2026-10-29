package pl.librus.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.librus.mobile.domain.models.Grade

@Composable
fun GradeBadge(
    grade: Grade,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = try {
        Color(parseColorHex(grade.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = grade.grade,
                color = bgColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            if (grade.weight > 0) {
                Text(
                    text = "w:${grade.weight.toInt()}",
                    color = bgColor.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

fun parseColorHex(hex: String): Long {
    val cleaned = hex.trim().removePrefix("#")
    return if (cleaned.length == 6) {
        ("FF$cleaned").toLong(16)
    } else if (cleaned.length == 8) {
        cleaned.toLong(16)
    } else {
        0xFF673AB7
    }
}
