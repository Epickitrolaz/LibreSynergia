package pl.librus.mobile

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import pl.librus.mobile.auth.*
import pl.librus.mobile.data.LibrusRepository
import pl.librus.mobile.data.MockDataProvider
import pl.librus.mobile.data.storage.*
import pl.librus.mobile.domain.calculations.AttendanceCalculator
import pl.librus.mobile.domain.models.*
import pl.librus.mobile.network.AttendanceParser
import pl.librus.mobile.network.LibrusLiveApiClient
import kotlin.test.*

class AttendanceTest {
    private val child = ChildAccount("42", "42u", "Jan Test", "42u", "token")
    private val session = UserSession(
        AuthBackendType.PORTAL_RODZINA, "parent@example.com", listOf(child), child,
        StudentProfile("42", "Jan", "Test")
    )
    private val cached = listOf(Attendance("cached", 2, "2026-10-26", "Math", AttendanceType.UNEXCUSED))
    private val typesPayload = """{"Types":[
        {"Id":100,"Name":"Obecność","Short":"ob","Standard":true},
        {"Id":"1","Name":"Nieobecność","Short":"nb","Standard":true},
        {"Id":2,"Name":"Spóźnienie","Short":"sp","Standard":true},
        {"Id":3,"Name":"Usprawiedliwiona","Short":"u","Standard":true},
        {"Id":4,"Name":"Zwolnienie","Short":"zw","Standard":true},
        {"Id":"91","Name":"Custom excused","Short":"cu","StandardType":{"Id":"3"},"ColorRGB":"123abc"}
    ]}"""

    @Test
    fun nativeAndCustomTypesAreNotClassifiedUsingDemoIds() {
        val types = assertNotNull(AttendanceParser.types(Json.parseToJsonElement(typesPayload).jsonObject))
        assertEquals(AttendanceKind.UNEXCUSED, types["1"]?.kind)
        assertEquals(AttendanceKind.LATE, types["2"]?.kind)
        assertEquals(AttendanceKind.EXEMPT, types["4"]?.kind)
        assertEquals(AttendanceKind.EXCUSED, types["91"]?.kind)
        assertEquals("#123abc", types["91"]?.colorHex)
        val records = types.values.mapIndexed { i, type -> Attendance("$i", i, "2026-10-26", "", type) }
        val stats = AttendanceCalculator.calculateStats(records)
        assertEquals(6, stats.totalHours)
        assertEquals(1, stats.presentCount)
        assertEquals(1, stats.lateCount)
        assertEquals(2, stats.excusedCount)
        assertEquals(1, stats.unexcusedCount)
        assertEquals(1, stats.exemptCount)
        assertEquals(40.0, stats.presencePercentage)
    }

    @Test
    fun fetchesBothBackendsAndResolvesLessonSubjects() = runBlocking {
        for (backend in listOf(AuthBackendType.PORTAL_RODZINA, AuthBackendType.GATEWAY_API_20)) {
            HttpClient(MockEngine { request ->
                assertEquals(if (backend == AuthBackendType.PORTAL_RODZINA) "api.librus.pl" else "synergia.librus.pl", request.url.host)
                assertEquals(if (backend == AuthBackendType.PORTAL_RODZINA) "Bearer token" else null, request.headers["Authorization"])
                respond(when (request.url.encodedPath.substringAfter("/3.0/", request.url.encodedPath.substringAfter("/2.0/"))) {
                    "Attendances" -> """{"Attendances":[
                        {"Id":"t41685","LessonNo":"3","Date":"2026-10-26","Type":{"Id":1},"Subject":null,"Lesson":{"Id":112}},
                        {"Id":67891,"LessonNo":2,"Date":"2026-10-27","Type":{"Id":"91"},"Subject":{"Id":"42"},"Lesson":null}
                    ]}"""
                    "Attendances/Types" -> typesPayload
                    "Subjects" -> """{"Subjects":[{"Id":42,"Name":"Matematyka"}]}"""
                    "Lessons" -> """{"Lessons":[{"Id":"112","Subject":{"Id":42}}]}"""
                    else -> error("Unexpected endpoint ${request.url}")
                })
            }).use { http ->
                val entries = assertNotNull(LibrusLiveApiClient(http).fetchAttendances(session.copy(backendType = backend)))
                assertEquals(listOf("67891", "t41685"), entries.map { it.id })
                assertTrue(entries.all { it.subjectName == "Matematyka" })
                assertEquals(AttendanceKind.UNEXCUSED, entries.last().type.kind)
            }
        }
    }

    @Test
    fun supportsDocumentedDictionaryFallback() = runBlocking {
        HttpClient(MockEngine { request ->
            respond(when {
                request.url.encodedPath.endsWith("/AttendanceTypes") -> """{"AttendanceTypes":[{"Id":3,"Name":"Excused","IsPresence":false,"IsAbsence":true}]}"""
                request.url.encodedPath.endsWith("/Attendances") -> """{"Attendances":[{"Id":1,"LessonNo":1,"Date":"2026-10-26","Type":{"Id":3}}]}"""
                else -> "{}"
            })
        }).use { http ->
            val entries = assertNotNull(LibrusLiveApiClient(http).fetchAttendances(session))
            assertEquals(AttendanceKind.EXCUSED, entries.single().type.kind)
            assertEquals("", entries.single().subjectName)
        }
    }

    @Test
    fun failedSyncKeepsSameOwnerCacheButClearsItForOtherOwners() = runBlocking {
        val storage = storage()
        HttpClient(MockEngine { respond("unavailable", HttpStatusCode.ServiceUnavailable) }).use { http ->
            val repository = LibrusRepository(storage, LibrusLiveApiClient(http))
            repository.loadDataForSession(session)
            assertTrue(repository.attendanceSyncFailed.value)
            assertEquals(cached, repository.attendances.value)
            assertEquals(1, repository.attendanceStats.value.unexcusedCount)
            assertEquals(cached, LibrusRepository(storage).attendances.value)
            for (other in listOf(
                session.copy(activeChild = child.copy(id = "other")),
                session.copy(identifier = "another@example.com"),
                session.copy(backendType = AuthBackendType.GATEWAY_API_20)
            )) {
                repository.applyCachedState(Json.decodeFromString(storage().getString(AuthManager.KEY_CACHED_STATE)!!))
                repository.loadDataForSession(other)
                assertTrue(repository.attendances.value.isEmpty())
                assertEquals(0, repository.attendanceStats.value.totalHours)
            }
        }
    }

    @Test
    fun emptySuccessReplacesAndPersistsCachedRecords() = runBlocking {
        val storage = storage()
        HttpClient(MockEngine { request ->
            respond(if (request.url.encodedPath.endsWith("/Attendances")) """{"Attendances":[]}""" else "{}")
        }).use { http ->
            val repository = LibrusRepository(storage, LibrusLiveApiClient(http))
            repository.loadDataForSession(session)
            assertFalse(repository.attendanceSyncFailed.value)
            assertTrue(repository.attendances.value.isEmpty())
            assertTrue(LibrusRepository(storage).attendances.value.isEmpty())
        }
    }

    @Test
    fun invalidResponsesNeverReplaceCacheWithPartialData() = runBlocking {
        for (payload in listOf("{}", "not json", """{"Attendances":null}""",
            """{"Attendances":[{"Id":"bad","Date":"invalid","Type":{"Id":1}}]}""",
            """{"Attendances":[{"Id":"unknown","Date":"2026-10-26","LessonNo":1,"Type":{"Id":999}}]}""")) {
            HttpClient(MockEngine { request ->
                respond(when {
                    request.url.encodedPath.endsWith("/Attendances") -> payload
                    request.url.encodedPath.endsWith("/Types") -> typesPayload
                    else -> "{}"
                })
            }).use { http ->
                val repository = LibrusRepository(storage(), LibrusLiveApiClient(http))
                repository.loadDataForSession(session)
                assertTrue(repository.attendanceSyncFailed.value)
                assertEquals(cached, repository.attendances.value)
            }
        }
    }

    @Test
    fun discardsLegacyDemoCacheButDemoModeStillWorks() = runBlocking {
        val repository = LibrusRepository(storage(owner = null))
        assertTrue(repository.attendances.value.isEmpty())
        repository.loadDataForSession(session)
        assertTrue(repository.attendances.value.isEmpty())
        repository.loadDataForSession(session.copy(backendType = AuthBackendType.DEMO_MOCK))
        assertEquals(MockDataProvider.attendances, repository.attendances.value)
    }

    @Test
    fun propagatesCancellation(): Unit = runBlocking {
        HttpClient(MockEngine { throw CancellationException("cancelled") }).use { http ->
            assertFailsWith<CancellationException> { LibrusLiveApiClient(http).fetchAttendances(session) }
        }
    }

    private fun storage(owner: String? = "${session.backendType}:${session.identifier}:${child.id}") =
        MemoryPersistentStorage().apply {
            putString(AuthManager.KEY_CACHED_STATE, Json.encodeToString(CachedAppState(
                attendances = cached, attendanceOwner = owner
            )))
        }
}
