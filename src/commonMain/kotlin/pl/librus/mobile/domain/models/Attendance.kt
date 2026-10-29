package pl.librus.mobile.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class AttendanceType(
    val id: String,
    val name: String,
    val shortName: String,
    val isPresence: Boolean,
    val isAbsence: Boolean,
    val colorHex: String = "#4CAF50"
) {
    companion object {
        val PRESENCE = AttendanceType("1", "Obecność", "ob", isPresence = true, isAbsence = false, colorHex = "#4CAF50")
        val UNEXCUSED = AttendanceType("2", "Nieobecność nieusprawiedliwiona", "nb", isPresence = false, isAbsence = true, colorHex = "#E53935")
        val EXCUSED = AttendanceType("3", "Nieobecność usprawiedliwiona", "u", isPresence = false, isAbsence = true, colorHex = "#1E88E5")
        val LATE = AttendanceType("4", "Spóźnienie", "sp", isPresence = true, isAbsence = false, colorHex = "#FB8C00")
        val EXEMPT = AttendanceType("5", "Zwolnienie", "zw", isPresence = false, isAbsence = false, colorHex = "#757575")
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
