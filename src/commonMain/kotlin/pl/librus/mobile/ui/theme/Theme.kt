package pl.librus.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LibrusPurple = Color(0xFF673AB7)
val LibrusPurpleDark = Color(0xFF512DA8)
val LibrusPurpleLight = Color(0xFFD1C4E9)

val PrimaryBlue = Color(0xFF1E88E5)
val AccentTeal = Color(0xFF00897B)
val GradeGood = Color(0xFF2E7D32)
val GradeMedium = Color(0xFFF57F17)
val GradePoor = Color(0xFFC62828)

private val LightColorScheme = lightColorScheme(
    primary = LibrusPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7F6),
    onPrimaryContainer = Color(0xFF311B92),
    secondary = PrimaryBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = Color(0xFF0D47A1),
    tertiary = AccentTeal,
    onTertiary = Color.White,
    background = Color(0xFFF8F9FD),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF0F0F5),
    onSurfaceVariant = Color(0xFF49454F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB39DDB),
    onPrimary = Color(0xFF311B92),
    primaryContainer = Color(0xFF512DA8),
    onPrimaryContainer = Color(0xFFEDE7F6),
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF0D47A1),
    secondaryContainer = Color(0xFF1565C0),
    onSecondaryContainer = Color(0xFFE3F2FD),
    tertiary = Color(0xFF80CBC4),
    onTertiary = Color(0xFF004D40),
    background = Color(0xFF121218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1E1E26),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2B2B36),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFFB39DDB),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF311B92),
    onPrimaryContainer = Color(0xFFEDE7F6),
    secondary = Color(0xFF90CAF9),
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF101010),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF181818),
    onSurfaceVariant = Color(0xFFCCCCCC)
)

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

@Composable
fun LibrusTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }

    val colorScheme = when {
        themeMode == AppThemeMode.AMOLED -> AmoledColorScheme
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
