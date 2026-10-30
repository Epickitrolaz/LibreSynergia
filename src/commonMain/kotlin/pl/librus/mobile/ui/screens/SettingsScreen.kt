package pl.librus.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.librus.mobile.auth.AuthBackendType
import pl.librus.mobile.auth.UserSession
import pl.librus.mobile.ui.AppLanguage
import pl.librus.mobile.ui.Strings
import pl.librus.mobile.ui.icons.AppIcon
import pl.librus.mobile.ui.icons.AppIconVectorDrawables
import pl.librus.mobile.ui.theme.AppThemeMode

@Composable
fun SettingsScreen(
    session: UserSession?,
    themeMode: AppThemeMode,
    language: AppLanguage,
    currentAppIcon: AppIcon = AppIcon.DEFAULT,
    strings: Strings,
    onThemeChange: (AppThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onAppIconChange: (AppIcon) -> Unit = {},
    onOpenAccountSwitcher: () -> Unit = {},
    onNavigateToNotices: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.close
                    )
                }
            }
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Account Details Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.accountSection,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${strings.loggedInAs}${session?.profile?.fullName ?: session?.identifier ?: "-"}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )

                val backendDesc = when (session?.backendType) {
                    AuthBackendType.PORTAL_RODZINA -> strings.backendPortal
                    AuthBackendType.GATEWAY_API_20 -> strings.backendGateway
                    AuthBackendType.DEMO_MOCK -> strings.backendDemo
                    null -> "-"
                }

                Text(
                    text = "${strings.authEngine}$backendDesc",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onOpenAccountSwitcher,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.switchAccount, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Switcher Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.languageSection,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChoiceButton(
                        text = "English",
                        selected = language == AppLanguage.ENGLISH,
                        onClick = { onLanguageChange(AppLanguage.ENGLISH) },
                        modifier = Modifier.weight(1f)
                    )
                    ChoiceButton(
                        text = "Polski",
                        selected = language == AppLanguage.POLISH,
                        onClick = { onLanguageChange(AppLanguage.POLISH) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance / Theme Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Brightness4,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.appearanceSection,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChoiceButton(
                        text = strings.themeSystem,
                        selected = themeMode == AppThemeMode.SYSTEM,
                        onClick = { onThemeChange(AppThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    ChoiceButton(
                        text = strings.themeLight,
                        selected = themeMode == AppThemeMode.LIGHT,
                        onClick = { onThemeChange(AppThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ChoiceButton(
                        text = strings.themeDark,
                        selected = themeMode == AppThemeMode.DARK,
                        onClick = { onThemeChange(AppThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                    ChoiceButton(
                        text = strings.themeAmoled,
                        selected = themeMode == AppThemeMode.AMOLED,
                        onClick = { onThemeChange(AppThemeMode.AMOLED) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Launcher Icon Switcher Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.appIconSection,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = strings.appIconDesc,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconOptionTile(
                        title = strings.appIconDefault,
                        selected = currentAppIcon == AppIcon.DEFAULT,
                        onClick = { onAppIconChange(AppIcon.DEFAULT) },
                        vector = AppIconVectorDrawables.DefaultMonogram,
                        backgroundColor = Color.Black,
                        modifier = Modifier.weight(1f)
                    )

                    IconOptionTile(
                        title = strings.appIconVariant,
                        selected = currentAppIcon == AppIcon.VARIANT,
                        onClick = { onAppIconChange(AppIcon.VARIANT) },
                        vector = AppIconVectorDrawables.VariantBook,
                        backgroundColor = Color(0xFF141414),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Information Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.aboutSection,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = strings.appVersion,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = strings.appDescription,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Logout Button
        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.logOut, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.height(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IconOptionTile(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    vector: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vector,
                    contentDescription = title,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (selected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Active",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}
