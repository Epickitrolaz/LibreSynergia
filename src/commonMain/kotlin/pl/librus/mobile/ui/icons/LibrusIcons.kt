package pl.librus.mobile.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Custom flat vector icons matching the clean design language of the app.
 * Raw Unicode emojis must never be used in the UI.
 */
object LibrusIcons {
    val DemoLaunch: ImageVector get() = Icons.Default.RocketLaunch
    val Celebration: ImageVector get() = Icons.Default.Celebration
    val HonorRibbon: ImageVector get() = Icons.Default.MilitaryTech
    val TargetGoal: ImageVector get() = Icons.Default.TrackChanges
    val ThumbUp: ImageVector get() = Icons.Default.ThumbUp
    val ArrowForward: ImageVector get() = Icons.AutoMirrored.Filled.ArrowForward
}
