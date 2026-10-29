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
import pl.librus.mobile.ui.components.GradeDetailDialog
import pl.librus.mobile.ui.screens.*
import pl.librus.mobile.ui.theme.AppThemeMode
import pl.librus.mobile.ui.theme.LibrusTheme

enum class NavigationItem(val title: String, val icon: ImageVector) {
    HOME("Pulpit", Icons.Default.Home),
    GRADES("Oceny", Icons.Default.FormatListNumbered),
    TIMETABLE("Plan", Icons.Default.CalendarToday),
    ATTENDANCE("Frekwencja", Icons.Default.CheckCircle),
    SIMULATOR("Kalkulator", Icons.Default.Calculate),
    NOTICES("Ogłoszenia", Icons.Default.Campaign),
    SETTINGS("Ustawienia", Icons.Default.Settings)
}

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

    var currentScreen by remember { mutableStateOf(NavigationItem.HOME) }
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
    val attendances by repository.attendances.collectAsState()
    val attendanceStats by repository.attendanceStats.collectAsState()
    val notices by repository.notices.collectAsState()
    val luckyNumber by repository.luckyNumber.collectAsState()

    LibrusTheme(themeMode = themeMode) {
        when (val state = authState) {
            is AuthState.Unauthenticated, is AuthState.Authenticating, is AuthState.Error -> {
                LoginScreen(
                    authState = state,
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
                                NavigationItem.HOME,
                                NavigationItem.GRADES,
                                NavigationItem.TIMETABLE,
                                NavigationItem.ATTENDANCE,
                                NavigationItem.SIMULATOR,
                                NavigationItem.SETTINGS
                            )
                            items.forEach { item ->
                                val isSelected = currentScreen == item
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentScreen = item },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(text = item.title, fontSize = 11.sp)
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
                                        text = backgroundSyncMessage ?: "Logowanie i aktualizacja...",
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
                                NavigationItem.HOME -> {
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
                                        onSwitchChild = { childId ->
                                            authManager.switchChild(childId)
                                            coroutineScope.launch {
                                                (authManager.state.value as? AuthState.Authenticated)?.let {
                                                    repository.loadDataForSession(it.session)
                                                }
                                            }
                                        },
                                        onNavigateToGrades = { currentScreen = NavigationItem.GRADES },
                                        onNavigateToTimetable = { currentScreen = NavigationItem.TIMETABLE },
                                        onNavigateToAttendance = { currentScreen = NavigationItem.ATTENDANCE },
                                        onNavigateToNotices = { currentScreen = NavigationItem.NOTICES },
                                        onGradeClick = { grade -> selectedGradeForDialog = grade }
                                    )
                                }
                                NavigationItem.GRADES -> {
                                    GradesScreen(
                                        groups = grades,
                                        onGradeClick = { grade -> selectedGradeForDialog = grade }
                                    )
                                }
                                NavigationItem.TIMETABLE -> {
                                    TimetableScreen(timetable = timetable)
                                }
                                NavigationItem.ATTENDANCE -> {
                                    AttendanceScreen(
                                        stats = attendanceStats,
                                        attendances = attendances
                                    )
                                }
                                NavigationItem.SIMULATOR -> {
                                    SimulatorScreen(groups = grades)
                                }
                                NavigationItem.NOTICES -> {
                                    NoticesScreen(
                                        notices = notices,
                                        onMarkAsRead = { noticeId ->
                                            repository.markNoticeAsRead(noticeId)
                                        }
                                    )
                                }
                                NavigationItem.SETTINGS -> {
                                    SettingsScreen(
                                        session = state.session,
                                        themeMode = themeMode,
                                        onThemeChange = {
                                            themeMode = it
                                            storage.putString("saved_theme", it.name)
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
