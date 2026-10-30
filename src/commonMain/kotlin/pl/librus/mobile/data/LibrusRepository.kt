package pl.librus.mobile.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.librus.mobile.auth.AuthBackendType
import pl.librus.mobile.auth.AuthManager
import pl.librus.mobile.auth.UserSession
import pl.librus.mobile.data.storage.CachedAppState
import pl.librus.mobile.data.storage.PersistentStorage
import pl.librus.mobile.domain.calculations.AttendanceCalculator
import pl.librus.mobile.domain.calculations.GradeCalculator
import pl.librus.mobile.domain.models.*
import pl.librus.mobile.network.LibrusLiveApiClient

class LibrusRepository(
    private val storage: PersistentStorage,
    private val liveApiClient: LibrusLiveApiClient? = null
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    private val _studentProfile = MutableStateFlow<StudentProfile?>(null)
    val studentProfile: StateFlow<StudentProfile?> = _studentProfile.asStateFlow()

    private val _grades = MutableStateFlow<List<SubjectGradeGroup>>(emptyList())
    val grades: StateFlow<List<SubjectGradeGroup>> = _grades.asStateFlow()

    private val _allGradesRaw = MutableStateFlow<List<Grade>>(emptyList())
    val allGradesRaw: StateFlow<List<Grade>> = _allGradesRaw.asStateFlow()

    private val _timetable = MutableStateFlow<List<TimetableDay>>(emptyList())
    val timetable: StateFlow<List<TimetableDay>> = _timetable.asStateFlow()
    private var timetableOwner: String? = null
    private val _timetableSyncFailed = MutableStateFlow(false)
    val timetableSyncFailed: StateFlow<Boolean> = _timetableSyncFailed.asStateFlow()

    private val _attendances = MutableStateFlow<List<Attendance>>(emptyList())
    val attendances: StateFlow<List<Attendance>> = _attendances.asStateFlow()
    private var attendanceOwner: String? = null
    private val _attendanceSyncFailed = MutableStateFlow(false)
    val attendanceSyncFailed: StateFlow<Boolean> = _attendanceSyncFailed.asStateFlow()

    private val _attendanceStats = MutableStateFlow(AttendanceStats())
    val attendanceStats: StateFlow<AttendanceStats> = _attendanceStats.asStateFlow()

    private val _notices = MutableStateFlow<List<SchoolNotice>>(emptyList())
    val notices: StateFlow<List<SchoolNotice>> = _notices.asStateFlow()

    private val _luckyNumber = MutableStateFlow<LuckyNumber?>(null)
    val luckyNumber: StateFlow<LuckyNumber?> = _luckyNumber.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        restoreFromCache()
    }

    private fun restoreFromCache() {
        val cachedJson = storage.getString(AuthManager.KEY_CACHED_STATE) ?: return
        try {
            val cached = json.decodeFromString<CachedAppState>(cachedJson)
            applyCachedState(cached)
        } catch (_: Exception) {}
    }

    fun applyCachedState(cached: CachedAppState) {
        _studentProfile.value = cached.profile
        _luckyNumber.value = cached.luckyNumber
        _allGradesRaw.value = cached.rawGrades

        val subjects = cached.subjects
        _grades.value = GradeCalculator.groupGradesBySubject(subjects, cached.rawGrades)

        timetableOwner = cached.timetableOwner
        _timetable.value = if (timetableOwner != null) cached.timetable else emptyList()
        attendanceOwner = cached.attendanceOwner
        _attendances.value = if (attendanceOwner != null) cached.attendances else emptyList()
        _attendanceStats.value = AttendanceCalculator.calculateStats(_attendances.value)

        val readIds = getReadNoticeIds()
        _notices.value = cached.notices.map {
            if (it.id in readIds) it.copy(isRead = true) else it
        }
    }

    private fun getReadNoticeIds(): Set<String> {
        val str = storage.getString(KEY_READ_NOTICES) ?: return emptySet()
        return try {
            json.decodeFromString<Set<String>>(str)
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun saveReadNoticeIds(ids: Set<String>) {
        try {
            storage.putString(KEY_READ_NOTICES, json.encodeToString(ids))
        } catch (_: Exception) {}
    }

    suspend fun loadDataForSession(session: UserSession) {
        _isLoading.value = true
        _timetableSyncFailed.value = false
        _attendanceSyncFailed.value = false
        try {
            val owner = "${session.backendType}:${session.identifier}:${session.activeChild.id}"
            if (attendanceOwner != owner) {
                _attendances.value = emptyList()
                _attendanceStats.value = AttendanceStats()
                attendanceOwner = owner
            }
            if (timetableOwner != owner) {
                _timetable.value = emptyList()
                timetableOwner = owner
            }
            if (session.backendType == AuthBackendType.DEMO_MOCK) {
                loadMockData(session)
            } else {
                loadLiveData(session)
            }
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun loadLiveData(session: UserSession) {
        try {
            val week = liveApiClient?.fetchTimetable(session)
            _timetableSyncFailed.value = week == null
            week?.let { _timetable.value = it }

            val attendance = liveApiClient?.fetchAttendances(session)
            _attendanceSyncFailed.value = attendance == null
            attendance?.let {
                _attendances.value = it
                _attendanceStats.value = AttendanceCalculator.calculateStats(it)
            }

            val me = liveApiClient?.fetchMe(session)
            val profile = me ?: session.profile
            _studentProfile.value = profile

            val lucky = liveApiClient?.fetchLuckyNumber(session)
            if (lucky != null) {
                _luckyNumber.value = lucky
            }

            val liveSubjects = liveApiClient?.fetchSubjects(session) ?: emptyList()
            // If live subjects fetched, use them; otherwise preserve current subjects
            val subjects = if (liveSubjects.isNotEmpty()) {
                liveSubjects
            } else {
                _grades.value.map { it.subject }.ifEmpty { emptyList() }
            }
            val subjectsMap = subjects.associateBy { it.id }

            val liveCategories = liveApiClient?.fetchGradeCategories(session) ?: emptyList()
            val categoriesMap = liveCategories.associateBy { it.id }

            val liveGrades = liveApiClient?.fetchGrades(session, subjectsMap, categoriesMap)
            val rawGrades = if (liveGrades != null) {
                _allGradesRaw.value = liveGrades
                liveGrades
            } else {
                _allGradesRaw.value
            }

            val groups = GradeCalculator.groupGradesBySubject(subjects, rawGrades)
            _grades.value = groups

            val readIds = getReadNoticeIds()
            val liveNotices = liveApiClient?.fetchSchoolNotices(session)
            if (liveNotices != null) {
                val sorted = liveNotices.sortedWith(
                    compareByDescending<SchoolNotice> { it.date }.thenByDescending { it.id }
                )
                _notices.value = sorted.map {
                    if (it.id in readIds) it.copy(isRead = true) else it
                }
            } else {
                _notices.value = _notices.value.sortedWith(
                    compareByDescending<SchoolNotice> { it.date }.thenByDescending { it.id }
                ).map {
                    if (it.id in readIds) it.copy(isRead = true) else it
                }
            }

            // Save to persistent storage cache (never with mock data for live sessions)
            val cacheState = CachedAppState(
                profile = profile,
                children = session.children,
                activeChild = session.activeChild,
                luckyNumber = _luckyNumber.value,
                subjects = subjects,
                categories = liveCategories,
                rawGrades = rawGrades,
                timetable = _timetable.value,
                timetableOwner = timetableOwner,
                attendances = _attendances.value,
                attendanceOwner = attendanceOwner,
                attendanceStats = _attendanceStats.value,
                notices = _notices.value
            )
            storage.putString(AuthManager.KEY_CACHED_STATE, json.encodeToString(cacheState))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Preserve cached live data on network/parsing failures; never switch to demo.
        }
    }

    private fun loadMockData(session: UserSession) {
        val child = session.activeChild
        val profile = MockDataProvider.getStudentProfile(child)
        _studentProfile.value = profile
        _luckyNumber.value = MockDataProvider.luckyNumber

        val rawGrades = if (child.id == "1001") {
            MockDataProvider.gradesJan
        } else {
            MockDataProvider.gradesJan.filterIndexed { index, _ -> index % 2 == 0 }
        }
        _allGradesRaw.value = rawGrades

        val groups = GradeCalculator.groupGradesBySubject(MockDataProvider.subjects, rawGrades)
        _grades.value = groups

        _timetable.value = MockDataProvider.timetableWeek
        _attendances.value = MockDataProvider.attendances
        _attendanceStats.value = AttendanceCalculator.calculateStats(MockDataProvider.attendances)
        _notices.value = MockDataProvider.notices

        // Save mock state to cache as well
        val cacheState = CachedAppState(
            profile = profile,
            children = session.children,
            activeChild = session.activeChild,
            luckyNumber = MockDataProvider.luckyNumber,
            subjects = MockDataProvider.subjects,
            categories = MockDataProvider.categories,
            rawGrades = rawGrades,
            timetable = _timetable.value,
            timetableOwner = timetableOwner,
            attendances = _attendances.value,
            attendanceOwner = attendanceOwner,
            attendanceStats = _attendanceStats.value,
            notices = _notices.value
        )
        storage.putString(AuthManager.KEY_CACHED_STATE, json.encodeToString(cacheState))
    }

    fun markNoticeAsRead(noticeId: String) {
        val readIds = getReadNoticeIds().toMutableSet()
        readIds.add(noticeId)
        saveReadNoticeIds(readIds)

        _notices.value = _notices.value.map {
            if (it.id == noticeId) it.copy(isRead = true) else it
        }

        // Also update cached state so it persists immediately
        storage.getString(AuthManager.KEY_CACHED_STATE)?.let { cachedJson ->
            try {
                val cached = json.decodeFromString<CachedAppState>(cachedJson)
                val updated = cached.copy(notices = _notices.value)
                storage.putString(AuthManager.KEY_CACHED_STATE, json.encodeToString(updated))
            } catch (_: Exception) {}
        }
    }

    companion object {
        const val KEY_READ_NOTICES = "librus_read_notice_ids"
    }
}
