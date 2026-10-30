package pl.librus.mobile.ui

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    POLISH("pl", "Polski")
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }

class Strings(val lang: AppLanguage) {
    val isPl = lang == AppLanguage.POLISH

    // App Branding
    val appName = "Librus Mobile"
    val appSubtitle = if (isPl) "Nowoczesny, niezależny klient dla Librus Synergia" else "Modern, cross-platform client for Librus Synergia"
    val appDescription = if (isPl) "Wieloplatformowy klient Android & iOS stworzony w Kotlin Multiplatform i Compose Multiplatform." else "Cross-platform mobile client for Android & iOS built with Kotlin Multiplatform and Compose Multiplatform."

    // Navigation
    val navHome = if (isPl) "Pulpit" else "Dashboard"
    val navGrades = if (isPl) "Oceny" else "Grades"
    val navSchedule = if (isPl) "Plan" else "Schedule"
    val navTimetable = if (isPl) "Plan" else "Timetable"
    val navAttendance = if (isPl) "Frekwencja" else "Attendance"
    val navSimulator = if (isPl) "Kalkulator" else "Calculator"
    val navNotices = if (isPl) "Ogłoszenia" else "Notices"
    val navSettings = if (isPl) "Ustawienia" else "Settings"
    val tabTimetable = if (isPl) "Plan lekcji" else "Timetable"
    val tabAttendance = if (isPl) "Frekwencja" else "Attendance"
    val tabGrades = if (isPl) "Oceny" else "Grades"
    val tabSimulator = if (isPl) "Symulator" else "Simulator"

    // Sync Banner
    val syncInProgress = if (isPl) "Logowanie i aktualizacja danych..." else "Signing in and updating data..."

    // Login Screen
    val demoTitle = if (isPl) "Przetestuj od razu bez logowania" else "Try Demo Mode Immediately"
    val demoDesc = if (isPl) "Wypróbuj aplikację z pełnymi danymi demonstracyjnymi (oceny, plan lekcji, zastępstwa)." else "Explore the app with preloaded realistic school data (grades, timetable, substitutions)."
    val demoButton = if (isPl) "Uruchom Tryb Demo" else "Launch Demo Mode"
    val tabPortal = if (isPl) "Portal Rodzina" else "Family Portal"
    val tabGateway = if (isPl) "Login Synergia" else "Synergia Login"
    val labelEmail = if (isPl) "Adres e-mail Portal Rodzina" else "Family Portal Email"
    val labelGatewayLogin = if (isPl) "Login szkolny (np. 1234567u)" else "School Login (e.g. 1234567u)"
    val labelPassword = if (isPl) "Hasło" else "Password"
    val actionSignIn = if (isPl) "Zaloguj się" else "Sign In"
    val passwordHide = if (isPl) "Ukryj hasło" else "Hide password"
    val passwordShow = if (isPl) "Pokaż hasło" else "Show password"

    // Home Screen
    val switchChild = if (isPl) "Zmień" else "Switch"
    val luckyNumber = if (isPl) "Szczęśliwy numerek" else "Lucky Number"
    val luckyNumberWon = if (isPl) "Masz szczęśliwy numerek!" else "You have today's lucky number!"
    val luckyNumberWonDesc = if (isPl) "Jesteś dzisiaj zwolniony z odpowiedzi ustnej!" else "You are exempt from oral questions today!"
    val yourRegisterNo = if (isPl) "Twój numer w dzienniku: " else "Your register number: "
    val gradeAverage = if (isPl) "Średnia ocen" else "GPA Average"
    val honorRoll = if (isPl) "Pasek czerwony" else "Honor Roll"
    val allSubjects = if (isPl) "Wszystkie przedmioty" else "All subjects"
    val attendance = if (isPl) "Frekwencja" else "Attendance"
    val hours = if (isPl) "godzin" else "hours"
    val newSchoolNotice = if (isPl) "Nowe ogłoszenie szkolne" else "New school notice"
    val timetableToday = if (isPl) "Plan lekcji: " else "Timetable: "
    val viewAll = if (isPl) "Zobacz cały" else "View all"
    val recentGrades = if (isPl) "Ostatnie oceny" else "Recent Grades"
    val all = if (isPl) "Wszystkie" else "All"
    val noLessonsToday = if (isPl) "Brak zaplanowanych lekcji na dzisiaj." else "No lessons scheduled for today."
    val noRecentGrades = if (isPl) "Brak ostatnich ocen." else "No recent grades."
    val badgeSubstitution = if (isPl) "Zastępstwo" else "Substitution"
    val badgeCancelled = if (isPl) "Odwołana" else "Cancelled"
    val classPrefix = if (isPl) "Klasa " else "Class "
    val numberShort = if (isPl) "Nr " else "No. "
    val studentDefault = if (isPl) "Uczeń" else "Student"

    // Grades Screen
    val gradesTitle = if (isPl) "Oceny szkolne" else "School Grades"
    val gpaSem1 = if (isPl) "Średnia ważona: Semestr I" else "Weighted Average: Semester 1"
    val gpaSem2 = if (isPl) "Średnia ważona: Semestr II" else "Weighted Average: Semester 2"
    val gpaAnnual = if (isPl) "Średnia roczna" else "Annual Average"
    val tabSem1 = if (isPl) "Semestr 1" else "Semester 1"
    val tabSem2 = if (isPl) "Semestr 2" else "Semester 2"
    val tabYear = if (isPl) "Cały rok" else "Full Year"
    val avgPrefix = if (isPl) "Śr: " else "Avg: "
    val noGrades = if (isPl) "Brak ocen" else "No grades"
    val noGradesInSemester = if (isPl) "Brak ocen w tym semestrze" else "No grades in this semester"
    val weightPrefix = if (isPl) "w:" else "w:"

    // Grade Detail Dialog
    val regularGrade = if (isPl) "Ocena bieżąca" else "Regular Grade"
    val gradeWeight = if (isPl) "Waga oceny" else "Grade Weight"
    val countsToAverage = if (isPl) " (liczy się do średniej)" else " (counts to average)"
    val doesNotCountToAverage = if (isPl) " (nie liczy się)" else " (does not count)"
    val entryDate = if (isPl) "Data wpisania" else "Date Added"
    val teacher = if (isPl) "Nauczyciel" else "Teacher"
    val teacherComment = if (isPl) "Komentarz nauczyciela" else "Teacher Comment"
    val close = if (isPl) "Zamknij" else "Close"

    // Timetable Screen
    val timetableTitle = if (isPl) "Plan lekcji" else "Timetable"
    val filterChangesOnly = if (isPl) "Tylko zmiany" else "Changes only"
    val lessonCancelled = if (isPl) "Lekcja odwołana" else "Lesson cancelled"
    val replaces = if (isPl) "Zamiast: " else "Replaces: "
    val noChangesMessage = if (isPl) "Brak zastępstw i odwołanych lekcji w tym dniu." else "No substitutions or cancelled lessons on this day."
    val noLessonsDay = if (isPl) "Brak lekcji w tym dniu." else "No lessons on this day."
    val timetableUnavailable = if (isPl) "Nie udało się pobrać planu. Zachowano dostępne dane offline." else "Could not load the timetable. Available offline data has been kept."
    val timetableNotLoaded = if (isPl) "Plan lekcji nie został jeszcze pobrany." else "The timetable has not been loaded yet."
    val timetableLoading = if (isPl) "Pobieranie planu lekcji…" else "Loading timetable…"
    val retryTimetable = if (isPl) "Spróbuj ponownie" else "Try again"
    val attendanceLoading = if (isPl) "Pobieranie frekwencji…" else "Loading attendance…"
    val attendanceUnavailable = if (isPl) "Nie udało się pobrać frekwencji. Zachowano dostępne dane offline." else "Could not load attendance. Available offline data has been kept."

    // Attendance Screen
    val attendanceTitle = if (isPl) "Frekwencja ogólna" else "Overall Attendance"
    val totalHoursRealized = if (isPl) "Łącznie zrealizowanych: " else "Total completed: "
    val hoursShort = if (isPl) "godz." else "hrs"
    val attPresent = if (isPl) "Obecność" else "Present"
    val attLate = if (isPl) "Spóźnienia" else "Late"
    val attExcused = if (isPl) "Usprawiedl." else "Excused"
    val attUnexcused = if (isPl) "Nieuspraw." else "Unexcused"
    val attExempt = if (isPl) "Zwolnienie" else "Exempt"
    val attendanceLog = if (isPl) "Dziennik obecności" else "Attendance Log"
    val noAttendanceRecords = if (isPl) "Brak wpisów o frekwencji." else "No attendance records."

    // Simulator Screen
    val simHeader = if (isPl) "Kalkulator i symulator ocen" else "Grade Calculator & Simulator"
    val simSubtitle = if (isPl) "Sprawdź jak przyszłe oceny wpłyną na Twoją średnią" else "Simulate how future grades will affect your GPA"
    val selectSubject = if (isPl) "Wybierz przedmiot:" else "Select subject:"
    val currentAvg = if (isPl) "Aktualna średnia" else "Current Average"
    val projectedAvg = if (isPl) "Przewidywana średnia" else "Projected Average"
    val simNewGradeHeader = if (isPl) "Symulacja nowej oceny:" else "Simulate a new grade:"
    val gradeOneToSix = if (isPl) "Ocena (1-6)" else "Grade (1-6)"
    val weight = if (isPl) "Waga" else "Weight"
    val addSimulated = if (isPl) "Dodaj" else "Add"
    val addedSimulatedGrades = if (isPl) "Dodane oceny próbne" else "Simulated grades added"
    val clear = if (isPl) "Wyczyść" else "Clear"
    val simGradeLabel = if (isPl) "Ocena: " else "Grade: "
    val simWeightLabel = if (isPl) ", waga: " else ", weight: "
    val targetCalcHeader = if (isPl) "Kalkulator celu: \"Co muszę dostać?\"" else "Target Goal: \"What grade do I need?\""
    val targetAvgLabel = if (isPl) "Docelowa średnia" else "Target Average"
    val examWeightLabel = if (isPl) "Waga sprawdzianu" else "Exam Weight"
    fun targetSuccess(req: Double) = if (isPl) "Musisz otrzymać ocenę minimum: $req" else "You need to score at least: $req"
    fun targetImpossible(req: Double, target: String) = if (isPl) "Średnia $target jest nieosiągalna z pojedynczego sprawdzianu (wymagałaby oceny $req)." else "Target average $target is unreachable from a single exam (would require $req)."
    fun targetAlreadyAbove(target: String) = if (isPl) "Już teraz masz średnią powyżej $target!" else "Your average is already above $target!"

    // Notices Screen
    val noticesTitle = if (isPl) "Ogłoszenia szkolne" else "School Notices"
    val noNotices = if (isPl) "Brak nowych ogłoszeń." else "No announcements."
    val newNoticeBadge = if (isPl) "Nowe" else "New"
    val markRead = if (isPl) "Przeczytane" else "Mark Read"

    // Settings Screen
    val settingsTitle = if (isPl) "Ustawienia" else "Settings"
    val accountSection = if (isPl) "Konto i profil" else "Account & Profile"
    val loggedInAs = if (isPl) "Zalogowano jako: " else "Logged in as: "
    val authEngine = if (isPl) "Silnik autoryzacji: " else "Auth Engine: "
    val backendPortal = if (isPl) "Konto Portal Rodzina (Bearer 3.0)" else "Family Portal Account (Bearer 3.0)"
    val backendGateway = if (isPl) "Synergia Gateway 2.0" else "Synergia Gateway 2.0"
    val backendDemo = if (isPl) "Tryb Demonstracyjny (Offline)" else "Demo Mode (Offline)"
    val languageSection = if (isPl) "Język aplikacji" else "App Language"
    val appearanceSection = if (isPl) "Wygląd i motyw" else "Appearance & Theme"
    val themeSystem = if (isPl) "System" else "System"
    val themeLight = if (isPl) "Jasny" else "Light"
    val themeDark = if (isPl) "Ciemny" else "Dark"
    val themeAmoled = "AMOLED"
    val appIconSection = if (isPl) "Ikona aplikacji" else "App Icon"
    val appIconDesc = if (isPl) "Wybierz wygląd ikony na ekranie głównym" else "Choose launcher icon on your home screen"
    val appIconDefault = if (isPl) "Domyślna (Monogram LS)" else "Default (LS Monogram)"
    val appIconVariant = if (isPl) "Izometryczna książka 3D" else "Isometric 3D Book"
    val aboutSection = if (isPl) "O aplikacji" else "About App"
    val appVersion = "Librus Synergia Mobile v1.0.0"
    val logOut = if (isPl) "Wyloguj się" else "Sign Out"

    // Back Exit Toast
    val backPressAgainToExit = if (isPl) "Naciśnij ponownie, aby wyjść z aplikacji" else "Press back again to exit"

    // Weekdays 2-letter abbreviations
    val dayPn = if (isPl) "Pn" else "Mo"
    val dayWt = if (isPl) "Wt" else "Tu"
    val daySr = if (isPl) "Śr" else "We"
    val dayCz = if (isPl) "Cz" else "Th"
    val dayPt = if (isPl) "Pt" else "Fr"
    val daySo = if (isPl) "So" else "Sa"
    val dayNd = if (isPl) "Nd" else "Su"

    // Lesson Status
    val statusLessonOngoing = if (isPl) "Lekcja trwa" else "Lesson in progress"
    val statusBreakOngoing = if (isPl) "Przerwa" else "Break"
    val statusBeforeSchool = if (isPl) "Przed lekcjami" else "Before school"
    val statusSchoolFinished = if (isPl) "Koniec lekcji na dzisiaj! Miłego odpoczynku." else "Lessons finished for today! Enjoy your rest."
    val statusNoLessonsToday = if (isPl) "Brak lekcji na dzisiaj" else "No lessons today"
    val statusToEnd = if (isPl) "do końca:" else "remaining:"
    val statusToStart = if (isPl) "do rozpoczęcia:" else "starts in:"
    val statusNextLesson = if (isPl) "Następna:" else "Next:"
    val statusCancelled = if (isPl) "ODWOŁANA" else "CANCELLED"
    val statusRoom = if (isPl) "sala" else "room"
    val statusLessonNo = if (isPl) "Lekcja" else "Lesson"
    val statusNextEffective = if (isPl) "Kolejna odbywająca się:" else "Next scheduled:"
    val currentTimeLabel = if (isPl) "Teraz" else "Now"

    // Account Switcher
    val accountSwitcherTitle = if (isPl) "Przełącz konto lub profil" else "Switch Account or Profile"
    val addAnotherAccount = if (isPl) "Dodaj kolejne konto" else "Add another account"
    val activeBadge = if (isPl) "Aktywne" else "Active"
    val linkedChildrenSection = if (isPl) "Profile uczniów (Portal)" else "Linked Student Profiles"
    val savedAccountsSection = if (isPl) "Zapisane konta" else "Saved Accounts"
    val switchAccount = if (isPl) "Przełącz konto" else "Switch Account"
    val removeAccount = if (isPl) "Usuń konto" else "Remove"

    // Notice Board Menu & Access
    val noticeBoardMenu = if (isPl) "Tablica ogłoszeń" else "Notice Board"
    val noticeBoardDesc = if (isPl) "Wszystkie komunikaty i ogłoszenia szkolne" else "School announcements & bulletins"
    val allNoticesRead = if (isPl) "Brak nowych ogłoszeń" else "No unread notices"
    val manualRefresh = if (isPl) "Odśwież" else "Refresh"
}
