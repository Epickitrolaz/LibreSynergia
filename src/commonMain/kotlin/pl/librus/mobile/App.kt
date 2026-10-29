package pl.librus.mobile

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pl.librus.mobile.auth.*
import pl.librus.mobile.data.LibrusRepository
import pl.librus.mobile.data.storage.MemoryPersistentStorage
import pl.librus.mobile.data.storage.PersistentStorage
import pl.librus.mobile.domain.models.Grade
import pl.librus.mobile.network.LibrusCookieStorage
import pl.librus.mobile.network.LibrusHttpClientFactory
import pl.librus.mobile.network.LibrusLiveApiClient
import pl.librus.mobile.ui.AppLanguage
import pl.librus.mobile.ui.Strings
import pl.librus.mobile.ui.components.GradeDetailDialog
import pl.librus.mobile.ui.screens.*
import pl.librus.mobile.ui.theme.AppThemeMode
import pl.librus.mobile.ui.theme.LibrusTheme

enum class ScreenKey {
    HOME,
    GRADES,
    TIMETABLE,
    ATTENDANCE,
    SIMULATOR,
    NOTICES,
    SETTINGS
}

data class NavItem(
    val key: ScreenKey,
    val title: (Strings) -> String,
    val icon: ImageVector
)

@Composable
fun App(
    storage: PersistentStorage = remember { MemoryPersistentStorage() }
) {
    val coroutineScope = rememberCoroutineScope()
    val authManager = remember(storage) { AuthManager(storage) }
    val cookieStorage = remember { LibrusCookieStorage() }
    val httpClient = remember { LibrusHttpClientFactory.createClient(cookieStorage) }
    val liveApiClient = remember(httpClient) { LibrusLiveApiClient(httpClient) }
    val repository = remember(storage, liveApiClient) { LibrusRepository(storage, liveApiClient) }
    val portalAuthClient = remember(httpClient) { PortalAuthClient(httpClient) }
    val gatewayAuthClient = remember(httpClient) { GatewayAuthClient(httpClient) }

    var currentScreen by remember { mutableStateOf(ScreenKey.HOME) }
    var language by remember {
        mutableStateOf(
            storage.getString("saved_language")?.let {
                runCatching { AppLanguage.valueOf(it) }.getOrNull()
            } ?: AppLanguage.ENGLISH
        )
    }
    val strings = remember(language) { Strings(language) }

    var themeMode by remember {
        mutableStateOf(
            storage.getString("saved_theme")?.let {
                runCatching { AppThemeMode.valueOf(it) }.getOrNull()
            } ?: AppThemeMode.SYSTEM
        )
    }
    var selectedGradeForDialog by remember { mutableStateOf<Grade?>(null) }

    val authState by authManager.state.collectAsState()
    val isBackgroundSyncing by authManager.isBackgroundSyncing.collectAsState()
    val backgroundSyncMessage by authManager.backgroundSyncMessage.collectAsState()

    // Trigger non-blocking background login and sync when app starts with stored credentials
    LaunchedEffect(Unit) {
        if (authManager.state.value is AuthState.Authenticated) {
            authManager.performBackgroundSync(portalAuthClient, gatewayAuthClient) { refreshedSession ->
                repository.loadDataForSession(refreshedSession)
            }
        }
    }

    val profile by repository.studentProfile.collectAsState()
    val grades by repository.grades.collectAsState()
    val recentGrades by repository.allGradesRaw.collectAsState()
    val timetable by repository.timetable.collectAsState()
    val timetableSyncFailed by repository.timetableSyncFailed.collectAsState()
    val attendanceSyncFailed by repository.attendanceSyncFailed.collectAsState()
    val isDataLoading by repository.isLoading.collectAsState()
    val attendances by repository.attendances.collectAsState()
    val attendanceStats by repository.attendanceStats.collectAsState()
    val notices by repository.notices.collectAsState()
    val luckyNumber by repository.luckyNumber.collectAsState()

    LibrusTheme(themeMode = themeMode) {
        when (val state = authState) {
            is AuthState.Unauthenticated, is AuthState.Authenticating, is AuthState.Error -> {
                LoginScreen(
                    authState = state,
                    strings = strings,
                    onLoginPortal = { email, pass ->
                        coroutineScope.launch {
                            authManager.loginPortal(email, pass, portalAuthClient)
                            val current = authManager.state.value as? AuthState.Authenticated
                            if (current != null) {
                                repository.loadDataForSession(current.session)
                            }
                        }
                    },
                    onLoginGateway = { login, pass ->
                        coroutineScope.launch {
                            authManager.loginGateway(login, pass, gatewayAuthClient)
                            val current = authManager.state.value as? AuthState.Authenticated
                            if (current != null) {
                                repository.loadDataForSession(current.session)
                            }
                        }
                    },
                    onLoginDemo = {
                        authManager.loginDemo()
                        val current = authManager.state.value as? AuthState.Authenticated
                        if (current != null) {
                            coroutineScope.launch {
                                repository.loadDataForSession(current.session)
                            }
                        }
                    },
                    onClearError = {
                        authManager.clearError()
                    }
                )
            }
            is AuthState.Authenticated -> {
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            val items = listOf(
                                NavItem(ScreenKey.HOME, { it.navHome }, Icons.Default.Home),
                                NavItem(ScreenKey.GRADES, { it.navGrades }, Icons.Default.FormatListNumbered),
                                NavItem(ScreenKey.TIMETABLE, { it.navTimetable }, Icons.Default.CalendarToday),
                                NavItem(ScreenKey.ATTENDANCE, { it.navAttendance }, Icons.Default.CheckCircle),
                                NavItem(ScreenKey.SIMULATOR, { it.navSimulator }, Icons.Default.Calculate),
                                NavItem(ScreenKey.SETTINGS, { it.navSettings }, Icons.Default.Settings)
                            )
                            items.forEach { item ->
                                val isSelected = currentScreen == item.key
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentScreen = item.key },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title(strings),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(text = item.title(strings), fontSize = 11.sp)
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Subtle non-intrusive background sync pill at the top of the content
                        AnimatedVisibility(
                            visible = isBackgroundSyncing,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                tonalElevation = 3.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = backgroundSyncMessage ?: strings.syncInProgress,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            when (currentScreen) {
                                ScreenKey.HOME -> {
                                    HomeScreen(
                                        profile = profile,
                                        children = state.session.children,
                                        activeChild = state.session.activeChild,
                                        luckyNumber = luckyNumber,
                                        grades = grades,
                                        recentGrades = recentGrades,
                                        timetable = timetable,
                                        attendanceStats = attendanceStats,
                                        notices = notices,
                                        strings = strings,
                                        onSwitchChild = { childId ->
                                            authManager.switchChild(childId)
                                            coroutineScope.launch {
                                                (authManager.state.value as? AuthState.Authenticated)?.let {
                                                    repository.loadDataForSession(it.session)
                                                }
                                            }
                                        },
                                        onNavigateToGrades = { currentScreen = ScreenKey.GRADES },
                                        onNavigateToTimetable = { currentScreen = ScreenKey.TIMETABLE },
                                        onNavigateToAttendance = { currentScreen = ScreenKey.ATTENDANCE },
                                        onNavigateToNotices = { currentScreen = ScreenKey.NOTICES },
                                        onGradeClick = { grade -> selectedGradeForDialog = grade }
                                    )
                                }
                                ScreenKey.GRADES -> {
                                    GradesScreen(
                                        groups = grades,
                                        strings = strings,
                                        onGradeClick = { grade -> selectedGradeForDialog = grade }
                                    )
                                }
                                ScreenKey.TIMETABLE -> {
                                    TimetableScreen(
                                        timetable = timetable,
                                        strings = strings,
                                        isLoading = isDataLoading,
                                        syncFailed = timetableSyncFailed,
                                        onRetry = {
                                            coroutineScope.launch {
                                                (authManager.state.value as? AuthState.Authenticated)?.let {
                                                    repository.loadDataForSession(it.session)
                                                }
                                            }
                                        }
                                    )
                                }
                                ScreenKey.ATTENDANCE -> {
                                    AttendanceScreen(
                                        stats = attendanceStats,
                                        attendances = attendances,
                                        strings = strings,
                                        isLoading = isDataLoading,
                                        syncFailed = attendanceSyncFailed,
                                        onRetry = {
                                            coroutineScope.launch {
                                                (authManager.state.value as? AuthState.Authenticated)?.let {
                                                    repository.loadDataForSession(it.session)
                                                }
                                            }
                                        }
                                    )
                                }
                                ScreenKey.SIMULATOR -> {
                                    SimulatorScreen(
                                        groups = grades,
                                        strings = strings
                                    )
                                }
                                ScreenKey.NOTICES -> {
                                    NoticesScreen(
                                        notices = notices,
                                        strings = strings,
                                        onMarkAsRead = { noticeId ->
                                            repository.markNoticeAsRead(noticeId)
                                        }
                                    )
                                }
                                ScreenKey.SETTINGS -> {
                                    SettingsScreen(
                                        session = state.session,
                                        themeMode = themeMode,
                                        language = language,
                                        strings = strings,
                                        onThemeChange = {
                                            themeMode = it
                                            storage.putString("saved_theme", it.name)
                                        },
                                        onLanguageChange = {
                                            language = it
                                            storage.putString("saved_language", it.name)
                                        },
                                        onLogout = {
                                            coroutineScope.launch {
                                                cookieStorage.clear()
                                            }
                                            authManager.logout()
                                        }
                                    )
                                }
                            }

                            // Floating Grade Detail Dialog
                            selectedGradeForDialog?.let { grade ->
                                GradeDetailDialog(
                                    grade = grade,
                                    strings = strings,
                                    onDismiss = { selectedGradeForDialog = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
