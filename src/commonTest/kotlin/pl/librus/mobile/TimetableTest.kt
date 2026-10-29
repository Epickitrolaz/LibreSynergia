package pl.librus.mobile

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.librus.mobile.auth.AuthBackendType
import pl.librus.mobile.auth.AuthManager
import pl.librus.mobile.auth.UserSession
import pl.librus.mobile.data.LibrusRepository
import pl.librus.mobile.data.MockDataProvider
import pl.librus.mobile.data.storage.CachedAppState
import pl.librus.mobile.data.storage.MemoryPersistentStorage
import pl.librus.mobile.domain.models.ChildAccount
import pl.librus.mobile.domain.models.StudentProfile
import pl.librus.mobile.domain.models.TimetableDay
import pl.librus.mobile.domain.models.TimetableSlot
import pl.librus.mobile.network.LibrusLiveApiClient
import pl.librus.mobile.network.TimetableParser
import kotlinx.serialization.json.jsonObject
import kotlin.test.*

class TimetableTest {
    private val monday = LocalDate(2026, 10, 26)
    private val child = ChildAccount("42", "42u", "Jan Test", "42u", "token")
    private val session = UserSession(
        AuthBackendType.PORTAL_RODZINA, "parent@example.com", listOf(child), child,
        StudentProfile("42", "Jan", "Test")
    )
    private val cachedWeek = listOf(TimetableDay(
        "2026-10-26", "Monday", listOf(TimetableSlot("cached", 1, "2026-10-26", "Real subject"))
    ))

    @Test
    fun parsesReferencesParallelGroupsAndChanges() {
        val root = Json.parseToJsonElement("""
            {"Timetable":{"2026-10-26":{
              "10":[{"LessonNo":"10","Subject":{"Id":2},"Teacher":null,"Classroom":null,
                       "IsCancelled":true}],
              "2":[{"LessonNo":2,"TimetableEntry":{"Id":"t41999"},
                    "Subject":{"Id":"1"},"Teacher":{"Id":10},"Classroom":{"Id":"204"},
                    "HourFrom":"08:55","HourTo":"09:40","IsSubstitution":true,
                    "OrgSubject":{"Id":2},"OrgTeacher":{"Id":"11"},
                    "SubstitutionNote":"Replacement","Group":"Group A"},
                   {"Subject":{"Id":2},"Group":{"Name":"Group B"}}]
            },"2026-10-27":[]}}
        """).jsonObject
        val week = assertNotNull(TimetableParser.parse(
            root, monday, mapOf("1" to "Math", "2" to "History"),
            mapOf("10" to "New Teacher", "11" to "Original Teacher"), mapOf("204" to "Room 204")
        ))
        assertEquals(7, week.size)
        assertEquals("2026-11-01", week.last().date)
        val lessons = week.first().lessons
        assertEquals(listOf(2, 2, 10), lessons.map { it.lessonNo })
        val replacement = lessons.first()
        assertEquals("Math", replacement.subjectName)
        assertEquals("New Teacher", replacement.teacherName)
        assertEquals("Room 204", replacement.classroom)
        assertEquals("08:55 - 09:40", replacement.timeRange)
        assertTrue(replacement.isSubstitution)
        assertEquals("History", replacement.orgSubjectName)
        assertEquals("Original Teacher", replacement.orgTeacherName)
        assertEquals("Replacement", replacement.substitutionNote)
        assertEquals(listOf("Group A", "Group B"), lessons.take(2).map { it.groupName })
        assertTrue(lessons.last().isCancelled)
        assertEquals("", lessons.last().teacherName)
        assertEquals(3, lessons.map { it.id }.distinct().size)
        assertTrue(week.drop(1).all { it.lessons.isEmpty() })
    }

    @Test
    fun missingPayloadIsFailureButEmptyWeekIsValid() {
        assertNull(TimetableParser.parse(Json.parseToJsonElement("{}").jsonObject, monday, emptyMap(), emptyMap(), emptyMap()))
        val week = assertNotNull(TimetableParser.parse(
            Json.parseToJsonElement("""{"Timetable":{}}""").jsonObject, monday, emptyMap(), emptyMap(), emptyMap()
        ))
        assertEquals(7, week.size)
        assertTrue(week.all { it.lessons.isEmpty() })
    }

    @Test
    fun requestsMondayAndUsesBackendAuthentication() = runBlocking {
        for (backend in listOf(AuthBackendType.PORTAL_RODZINA, AuthBackendType.GATEWAY_API_20)) {
            val paths = mutableListOf<String>()
            val engine = MockEngine { request ->
                paths += request.url.encodedPath.substringAfterLast('/')
                assertEquals(
                    if (backend == AuthBackendType.PORTAL_RODZINA) "api.librus.pl" else "synergia.librus.pl",
                    request.url.host
                )
                assertEquals(
                    if (backend == AuthBackendType.PORTAL_RODZINA) "Bearer token" else null,
                    request.headers["Authorization"]
                )
                val payload = when (paths.last()) {
                    "Timetables" -> {
                        assertEquals("2026-10-26", request.url.parameters["weekStart"])
                        """{"Timetable":{"2026-10-26":{"1":[{"Subject":{"Id":1},"Teacher":{"Id":"2"},"Classroom":{"Id":3}}]}}}"""
                    }
                    "Subjects" -> """{"Subjects":[{"Id":"1","Name":"Math"}]}"""
                    "Users" -> """{"Users":[{"Id":2,"FirstName":"Jan","LastName":"Test"}]}"""
                    "Classrooms" -> """{"Classrooms":[{"Id":"3","Name":"204"}]}"""
                    else -> error("Unexpected endpoint")
                }
                respond(payload)
            }
            HttpClient(engine).use { http ->
                val week = assertNotNull(LibrusLiveApiClient(http).fetchTimetable(
                    session.copy(backendType = backend), LocalDate(2026, 11, 1)
                ))
                assertEquals("Math", week.first().lessons.first().subjectName)
                assertEquals("Jan Test", week.first().lessons.first().teacherName)
                assertEquals("204", week.first().lessons.first().classroom)
                assertEquals(listOf("Timetables", "Subjects", "Users", "Classrooms"), paths)
            }
        }
    }

    @Test
    fun networkFailurePreservesSameStudentCacheButNotAnotherStudents() = runBlocking {
        val storage = cachedStorage()
        HttpClient(MockEngine { respond("unavailable", HttpStatusCode.ServiceUnavailable) }).use { http ->
            val repository = LibrusRepository(storage, LibrusLiveApiClient(http))
            repository.loadDataForSession(session)
            assertEquals(cachedWeek, repository.timetable.value)
            assertFalse(repository.isLoading.value)
            assertEquals(cachedWeek, LibrusRepository(storage).timetable.value)
            repository.loadDataForSession(session.copy(activeChild = child.copy(id = "other")))
            assertTrue(repository.timetable.value.isEmpty())
        }
    }

    @Test
    fun successfulEmptyWeekReplacesAndPersistsOldSchedule() = runBlocking {
        val storage = cachedStorage()
        HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("/Timetables")) respond("""{"Timetable":{}}""")
            else respond("{}")
        }).use { http ->
            val repository = LibrusRepository(storage, LibrusLiveApiClient(http))
            repository.loadDataForSession(session)
            assertEquals(7, repository.timetable.value.size)
            assertTrue(repository.timetable.value.all { it.lessons.isEmpty() })
            assertEquals(repository.timetable.value, LibrusRepository(storage).timetable.value)
        }
    }

    @Test
    fun legacyHardcodedCacheIsDiscardedAndDemoRemainsAvailable() = runBlocking {
        val repository = LibrusRepository(cachedStorage(owner = null))
        assertTrue(repository.timetable.value.isEmpty())
        repository.loadDataForSession(session)
        assertTrue(repository.timetable.value.isEmpty())
        repository.loadDataForSession(session.copy(backendType = AuthBackendType.DEMO_MOCK))
        assertEquals(MockDataProvider.timetableWeek, repository.timetable.value)
    }

    @Test
    fun cancellationIsNotTurnedIntoDemoData() = runBlocking {
        HttpClient(MockEngine { throw CancellationException("cancelled") }).use { http ->
            val repository = LibrusRepository(cachedStorage(), LibrusLiveApiClient(http))
            assertFailsWith<CancellationException> { repository.loadDataForSession(session) }
            assertEquals(cachedWeek, repository.timetable.value)
            assertFalse(repository.isLoading.value)
        }
    }

    private fun cachedStorage(
        owner: String? = "${session.backendType}:${session.identifier}:${child.id}"
    ) = MemoryPersistentStorage().apply {
        putString(AuthManager.KEY_CACHED_STATE, Json.encodeToString(CachedAppState(
            profile = session.profile, children = session.children, activeChild = child,
            timetable = cachedWeek, timetableOwner = owner
        )))
    }
}
