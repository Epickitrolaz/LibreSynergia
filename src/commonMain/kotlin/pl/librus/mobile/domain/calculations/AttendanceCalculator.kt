package pl.librus.mobile.domain.calculations

import pl.librus.mobile.domain.models.Attendance
import pl.librus.mobile.domain.models.AttendanceStats
import kotlin.math.roundToInt

object AttendanceCalculator {

    fun calculateStats(attendances: List<Attendance>): AttendanceStats {
        if (attendances.isEmpty()) return AttendanceStats()

        var present = 0
        var excused = 0
        var unexcused = 0
        var late = 0
        var exempt = 0

        for (a in attendances) {
            when (a.type.id) {
                "1" -> present++
                "2" -> unexcused++
                "3" -> excused++
                "4" -> late++
                "5" -> exempt++
                else -> {
                    if (a.type.isPresence) present++
                    else if (a.type.isAbsence) unexcused++
                }
            }
        }

        val totalHours = attendances.size
        val countableHours = totalHours - exempt

        val presencePct = if (countableHours > 0) {
            val presentTotal = present + late
            val pct = (presentTotal.toDouble() / countableHours.toDouble()) * 100.0
            (pct * 10.0).roundToInt() / 10.0
        } else {
            100.0
        }

        return AttendanceStats(
            totalHours = totalHours,
            presentCount = present,
            excusedCount = excused,
            unexcusedCount = unexcused,
            lateCount = late,
            exemptCount = exempt,
            presencePercentage = presencePct
        )
    }
}
