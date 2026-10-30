package pl.librus.mobile

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import pl.librus.mobile.platform.PlatformBackHandler
import pl.librus.mobile.platform.exitPlatformApp
import pl.librus.mobile.platform.showPlatformToast
import pl.librus.mobile.ui.AppLanguage
import pl.librus.mobile.ui.Strings
import pl.librus.mobile.ui.components.AccountSwitcherDialog
import pl.librus.mobile.ui.components.GradeDetailDialog
import pl.librus.mobile.ui.icons.AppIcon
import pl.librus.mobile.ui.icons.AppIconManager
import pl.librus.mobile.ui.icons.DefaultAppIconManager
import pl.librus.mobile.ui.screens.*
import pl.librus.mobile.ui.theme.AppThemeMode
import pl.librus.mobile.ui.theme.LibrusTheme

enum class ScreenKey {
    HOME,
    GRADES,
    SCHEDULE,
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
    storage: PersistentStorage = remember { MemoryPersistentStorage() },
    iconManager: AppIconManager = remember(storage) { DefaultAppIconManager(storage) }
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
    var gradesSubTab by remember { mutableStateOf(0) }
    var scheduleSubTab by remember { mutableStateOf(0) }
    var currentAppIcon by remember { mutableStateOf(iconManager.getCurrentIcon()) }
    var showAccountSwitcherDialog by remember { mutableStateOf(false) }
    var isAddingAccount by remember { mutableStateOf(false) }
    var isManualRefreshing by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableStateOf(0L) }

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
    val savedAccounts by authManager.savedAccounts.collectAsState()

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

    val unreadNoticesCount = remember(notices) { notices.count { !it.isRead } }

    fun triggerRefresh() {
        coroutineScope.launch {
            isManualRefreshing = true
            try {
                authManager.performBackgroundSync(portalAuthClient, gatewayAuthClient) { refreshedSession ->
                    repository.loadDataForSession(refreshedSession)
                }
                (authManager.state.value as? AuthState.Authenticated)?.let {
                    repository.loadDataForSession(it.session)
                }
            } finally {
                isManualRefreshing = false
            }
        }
    }

    // Platform Back Navigation Handler (Issue 4)
    PlatformBackHandler(enabled = true) {
        if (selectedGradeForDialog != null) {
            selectedGradeForDialog = null
        } else if (showAccountSwitcherDialog) {
            showAccountSwitcherDialog = false
        } else if (isAddingAccount) {
            isAddingAccount = false
        } else if (currentScreen == ScreenKey.NOTICES) {
            currentScreen = ScreenKey.HOME
        } else if (currentScreen == ScreenKey.GRADES && gradesSubTab != 0) {
            gradesSubTab = 0
        } else if (currentScreen == ScreenKey.SCHEDULE && scheduleSubTab != 0) {
            scheduleSubTab = 0
        } else if (currentScreen != ScreenKey.HOME) {
            currentScreen = ScreenKey.HOME
        } else {
            // At root of the navigation tree (Dashboard)
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            if (now - lastBackPressTime < 2000L) {
                exitPlatformApp()
            } else {
                lastBackPressTime = now
                showPlatformToast(strings.backPressAgainToExit)
            }
        }
    }

    LibrusTheme(themeMode = themeMode) {
        if (isAddingAccount) {
            // Add Account Screen Mode
            Scaffold(
                topBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            IconButton(onClick = { isAddingAccount = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.close)
                            }
                            Text(
                                text = strings.addAnotherAccount,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    LoginScreen(
                        authState = authState,
                        strings = strings,
                        onLoginPortal = { email, pass ->
                            coroutineScope.launch {
                                authManager.loginPortal(email, pass, portalAuthClient)
                                val current = authManager.state.value as? AuthState.Authenticated
                                if (current != null) {
                                    isAddingAccount = false
                                    repository.loadDataForSession(current.session)
                                }
                            }
                        },
                        onLoginGateway = { login, pass ->
                            coroutineScope.launch {
                                authManager.loginGateway(login, pass, gatewayAuthClient)
                                val current = authManager.state.value as? AuthState.Authenticated
                                if (current != null) {
                                    isAddingAccount = false
                                    repository.loadDataForSession(current.session)
                                }
                            }
                        },
                        onLoginDemo = {
                            authManager.loginDemo()
                            val current = authManager.state.value as? AuthState.Authenticated
                            if (current != null) {
                                isAddingAccount = false
                                coroutineScope.launch {
                                    repository.loadDataForSession(current.session)
                                }
                            }
                        },
                        onClearError = { authManager.clearError() }
                    )
                }
            }
            return@LibrusTheme
        }

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
                    topBar = {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        showAccountSwitcherDialog = true
                                    }
                                ) {
                                    Text(
                                        text = "LibreSynergia",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = strings.accountSwitcherTitle,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Notice Board Icon Button with Badge (Issue 7)
                                    IconButton(
                                        onClick = { currentScreen = ScreenKey.NOTICES }
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (unreadNoticesCount > 0) {
                                                    Badge {
                                                        Text("$unreadNoticesCount")
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (unreadNoticesCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                                contentDescription = strings.noticeBoardMenu,
                                                tint = if (unreadNoticesCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Manual Refresh Icon Button (Issue 13)
                                    IconButton(
                                        onClick = { triggerRefresh() },
                                        enabled = !isManualRefreshing && !isDataLoading
                                    ) {
                                        if (isManualRefreshing || isDataLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = strings.manualRefresh,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp,
                                windowInsets = WindowInsets(0, 0, 0, 0),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            ) {
                                val items = listOf(
                                    NavItem(ScreenKey.HOME, { it.navHome }, Icons.Default.Home),
                                    NavItem(ScreenKey.GRADES, { it.navGrades }, Icons.Default.FormatListNumbered),
                                    NavItem(ScreenKey.SCHEDULE, { it.navSchedule }, Icons.Default.CalendarToday),
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
                                            Text(
                                                text = item.title(strings),
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        alwaysShowLabel = true
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Background sync indicator banner
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
                                        onOpenAccountSwitcher = { showAccountSwitcherDialog = true },
                                        onNavigateToGrades = {
                                            currentScreen = ScreenKey.GRADES
                                            gradesSubTab = 0
                                        },
                                        onNavigateToSimulator = {
                                            currentScreen = ScreenKey.GRADES
                                            gradesSubTab = 1
                                        },
                                        onNavigateToTimetable = {
                                            currentScreen = ScreenKey.SCHEDULE
                                            scheduleSubTab = 0
                                        },
                                        onNavigateToAttendance = {
                                            currentScreen = ScreenKey.SCHEDULE
                                            scheduleSubTab = 1
                                        },
                                        onNavigateToNotices = { currentScreen = ScreenKey.NOTICES },
                                        onGradeClick = { grade -> selectedGradeForDialog = grade },
                                        isRefreshing = isManualRefreshing,
                                        onRefresh = { triggerRefresh() }
                                    )
                                }
                                ScreenKey.GRADES -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        TabRow(
                                            selectedTabIndex = gradesSubTab,
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            Tab(
                                                selected = gradesSubTab == 0,
                                                onClick = { gradesSubTab = 0 },
                                                text = { Text(strings.tabGrades, fontWeight = FontWeight.SemiBold) },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.FormatListNumbered,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            )
                                            Tab(
                                                selected = gradesSubTab == 1,
                                                onClick = { gradesSubTab = 1 },
                                                text = { Text(strings.tabSimulator, fontWeight = FontWeight.SemiBold) },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.Calculate,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            when (gradesSubTab) {
                                                0 -> GradesScreen(
                                                    groups = grades,
                                                    strings = strings,
                                                    onGradeClick = { grade -> selectedGradeForDialog = grade },
                                                    isRefreshing = isManualRefreshing,
                                                    onRefresh = { triggerRefresh() }
                                                )
                                                else -> SimulatorScreen(
                                                    groups = grades,
                                                    strings = strings
                                                )
                                            }
                                        }
                                    }
                                }
                                ScreenKey.SCHEDULE -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        TabRow(
                                            selectedTabIndex = scheduleSubTab,
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            Tab(
                                                selected = scheduleSubTab == 0,
                                                onClick = { scheduleSubTab = 0 },
                                                text = { Text(strings.tabTimetable, fontWeight = FontWeight.SemiBold) },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.CalendarToday,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            )
                                            Tab(
                                                selected = scheduleSubTab == 1,
                                                onClick = { scheduleSubTab = 1 },
                                                text = { Text(strings.tabAttendance, fontWeight = FontWeight.SemiBold) },
                                                icon = {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            )
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            when (scheduleSubTab) {
                                                0 -> TimetableScreen(
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
                                                    },
                                                    isRefreshing = isManualRefreshing,
                                                    onRefresh = { triggerRefresh() }
                                                )
                                                else -> AttendanceScreen(
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
                                        }
                                    }
                                }
                                ScreenKey.NOTICES -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 2.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                IconButton(onClick = { currentScreen = ScreenKey.HOME }) {
                                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                                }
                                                Text(
                                                    text = strings.noticesTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            NoticesScreen(
                                                notices = notices,
                                                strings = strings,
                                                onMarkAsRead = { noticeId ->
                                                    repository.markNoticeAsRead(noticeId)
                                                }
                                            )
                                        }
                                    }
                                }
                                ScreenKey.SETTINGS -> {
                                    SettingsScreen(
                                        session = state.session,
                                        themeMode = themeMode,
                                        language = language,
                                        currentAppIcon = currentAppIcon,
                                        strings = strings,
                                        onThemeChange = {
                                            themeMode = it
                                            storage.putString("saved_theme", it.name)
                                        },
                                        onLanguageChange = {
                                            language = it
                                            storage.putString("saved_language", it.name)
                                        },
                                        onAppIconChange = {
                                            currentAppIcon = it
                                            iconManager.setAppIcon(it)
                                        },
                                        onOpenAccountSwitcher = { showAccountSwitcherDialog = true },
                                        onNavigateToNotices = { currentScreen = ScreenKey.NOTICES },
                                        onLogout = {
                                            coroutineScope.launch {
                                                cookieStorage.clear()
                                            }
                                            authManager.logout()
                                        }
                                    )
                                }
                            }

                            // Grade Detail Dialog
                            selectedGradeForDialog?.let { grade ->
                                GradeDetailDialog(
                                    grade = grade,
                                    strings = strings,
                                    onDismiss = { selectedGradeForDialog = null }
                                )
                            }

                            // Multi-Account Switcher Dialog (Issue 11)
                            if (showAccountSwitcherDialog) {
                                AccountSwitcherDialog(
                                    currentSession = state.session,
                                    savedAccounts = savedAccounts,
                                    strings = strings,
                                    onDismiss = { showAccountSwitcherDialog = false },
                                    onSwitchChild = { childId ->
                                        authManager.switchChild(childId)
                                        coroutineScope.launch {
                                            (authManager.state.value as? AuthState.Authenticated)?.let {
                                                repository.loadDataForSession(it.session)
                                            }
                                        }
                                    },
                                    onSwitchAccount = { accountId ->
                                        authManager.switchSavedAccount(accountId) { newSession ->
                                            coroutineScope.launch {
                                                repository.loadDataForSession(newSession)
                                            }
                                        }
                                    },
                                    onAddAccount = {
                                        showAccountSwitcherDialog = false
                                        isAddingAccount = true
                                    },
                                    onRemoveAccount = { accountId ->
                                        authManager.removeAccount(accountId)
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
