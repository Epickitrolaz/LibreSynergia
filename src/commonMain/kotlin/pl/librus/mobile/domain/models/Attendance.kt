package pl.librus.mobile.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class AttendanceKind { PRESENT, UNEXCUSED, EXCUSED, LATE, EXEMPT, UNKNOWN }

@Serializable
data class AttendanceType(
    val id: String,
    val name: String,
    val shortName: String,
    val isPresence: Boolean,
    val isAbsence: Boolean,
    val colorHex: String = "#4CAF50",
    val kind: AttendanceKind = AttendanceKind.UNKNOWN
) {
    companion object {
        val PRESENCE = AttendanceType("1", "Obecność", "ob", true, false, "#4CAF50", AttendanceKind.PRESENT)
        val UNEXCUSED = AttendanceType("2", "Nieobecność nieusprawiedliwiona", "nb", false, true, "#E53935", AttendanceKind.UNEXCUSED)
        val EXCUSED = AttendanceType("3", "Nieobecność usprawiedliwiona", "u", false, true, "#1E88E5", AttendanceKind.EXCUSED)
        val LATE = AttendanceType("4", "Spóźnienie", "sp", true, false, "#FB8C00", AttendanceKind.LATE)
        val EXEMPT = AttendanceType("5", "Zwolnienie", "zw", false, false, "#757575", AttendanceKind.EXEMPT)
    }
}

@Serializable
data class Attendance(
    val id: String,
    val lessonNo: Int,
    val date: String,
    val subjectName: String,
    val type: AttendanceType
)

@Serializable
data class AttendanceStats(
    val totalHours: Int = 0,
    val presentCount: Int = 0,
    val excusedCount: Int = 0,
    val unexcusedCount: Int = 0,
    val lateCount: Int = 0,
    val exemptCount: Int = 0,
    val presencePercentage: Double = 100.0
)
