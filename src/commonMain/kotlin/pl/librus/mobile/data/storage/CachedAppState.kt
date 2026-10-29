package pl.librus.mobile.data.storage

import kotlinx.serialization.Serializable
import pl.librus.mobile.domain.models.*

@Serializable
data class CachedAppState(
    val profile: StudentProfile? = null,
    val children: List<ChildAccount> = emptyList(),
    val activeChild: ChildAccount? = null,
    val luckyNumber: LuckyNumber? = null,
    val subjects: List<Subject> = emptyList(),
    val categories: List<GradeCategory> = emptyList(),
    val rawGrades: List<Grade> = emptyList(),
    val timetable: List<TimetableDay> = emptyList(),
    // Missing on legacy caches, whose timetable contained hardcoded demo data.
    val timetableOwner: String? = null,
    val attendances: List<Attendance> = emptyList(),
    val attendanceOwner: String? = null,
    val attendanceStats: AttendanceStats = AttendanceStats(),
    val notices: List<SchoolNotice> = emptyList()
)
