package pl.librus.mobile

import kotlinx.datetime.DayOfWeek
import pl.librus.mobile.ui.AppLanguage
import pl.librus.mobile.ui.Strings
import kotlin.test.*

class LocalizationAndBrandingTest {

    @Test
    fun testBrandingNameIsLibreSynergia() {
        val stringsEn = Strings(AppLanguage.ENGLISH)
        val stringsPl = Strings(AppLanguage.POLISH)

        assertEquals("LibreSynergia", stringsEn.appName)
        assertEquals("LibreSynergia", stringsPl.appName)

        assertTrue(stringsEn.appVersion.startsWith("LibreSynergia"))
        assertTrue(stringsPl.appVersion.startsWith("LibreSynergia"))

        assertFalse(stringsEn.appName.contains("LibrusMobile", ignoreCase = true))
        assertFalse(stringsPl.appName.contains("LibrusMobile", ignoreCase = true))
        assertFalse(stringsEn.appName.contains("Librus Mobile", ignoreCase = true))
    }

    @Test
    fun testRefreshAndSyncTranslations() {
        val stringsEn = Strings(AppLanguage.ENGLISH)
        val stringsPl = Strings(AppLanguage.POLISH)

        // Background sync and refresh text must differ based on language setting
        assertEquals("Signing in and updating data...", stringsEn.syncInProgress)
        assertEquals("Logowanie i aktualizacja danych...", stringsPl.syncInProgress)

        assertEquals("Refresh", stringsEn.manualRefresh)
        assertEquals("Odśwież", stringsPl.manualRefresh)

        assertEquals("Loading timetable…", stringsEn.timetableLoading)
        assertEquals("Pobieranie planu lekcji…", stringsPl.timetableLoading)

        assertEquals("Loading attendance…", stringsEn.attendanceLoading)
        assertEquals("Pobieranie frekwencji…", stringsPl.attendanceLoading)
    }

    @Test
    fun testWeekdayNamesTranslation() {
        val stringsEn = Strings(AppLanguage.ENGLISH)
        val stringsPl = Strings(AppLanguage.POLISH)

        assertEquals("Monday", stringsEn.getDayName(DayOfWeek.MONDAY))
        assertEquals("Poniedziałek", stringsPl.getDayName(DayOfWeek.MONDAY))

        assertEquals("Thursday", stringsEn.getDayName(DayOfWeek.THURSDAY))
        assertEquals("Czwartek", stringsPl.getDayName(DayOfWeek.THURSDAY))

        assertEquals("Friday", stringsEn.getDayName(DayOfWeek.FRIDAY))
        assertEquals("Piątek", stringsPl.getDayName(DayOfWeek.FRIDAY))

        assertEquals("Mo", stringsEn.getTwoLetterDay(DayOfWeek.MONDAY))
        assertEquals("Pn", stringsPl.getTwoLetterDay(DayOfWeek.MONDAY))

        assertEquals("Th", stringsEn.getTwoLetterDay(DayOfWeek.THURSDAY))
        assertEquals("Cz", stringsPl.getTwoLetterDay(DayOfWeek.THURSDAY))
    }

    @Test
    fun testSearchAndDropdownTranslations() {
        val stringsEn = Strings(AppLanguage.ENGLISH)
        val stringsPl = Strings(AppLanguage.POLISH)

        assertEquals("Search...", stringsEn.searchPlaceholder)
        assertEquals("Szukaj...", stringsPl.searchPlaceholder)

        assertEquals("No search results found.", stringsEn.noSearchResults)
        assertEquals("Brak wyników wyszukiwania.", stringsPl.noSearchResults)

        assertEquals("Assignment", stringsEn.assignmentDefault)
        assertEquals("Zadanie", stringsPl.assignmentDefault)
    }
}
