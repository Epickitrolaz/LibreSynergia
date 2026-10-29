package pl.librus.mobile

import pl.librus.mobile.domain.calculations.AttendanceCalculator
import pl.librus.mobile.domain.calculations.GradeCalculator
import pl.librus.mobile.domain.models.Attendance
import pl.librus.mobile.domain.models.AttendanceType
import pl.librus.mobile.domain.models.Grade
import pl.librus.mobile.domain.models.Subject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CalculatorsTest {

    @Test
    fun testParseGradeValue() {
        assertEquals(5.0, Grade.parseGradeValue("5"))
        assertEquals(5.5, Grade.parseGradeValue("5+"))
        assertEquals(4.75, Grade.parseGradeValue("5-"))
        assertEquals(3.5, Grade.parseGradeValue("3+"))
        assertEquals(2.75, Grade.parseGradeValue("3-"))
        assertEquals(1.0, Grade.parseGradeValue("1"))
        assertNull(Grade.parseGradeValue("np"))
        assertNull(Grade.parseGradeValue("bz"))
        assertNull(Grade.parseGradeValue("+"))
    }

    @Test
    fun testWeightedAverageCalculation() {
        val mathSubject = Subject(id = "1", name = "Matematyka")
        val grades = listOf(
            Grade(id = "1", grade = "5", subjectId = "1", subjectName = "Matematyka", weight = 3.0), // 15.0
            Grade(id = "2", grade = "4", subjectId = "1", subjectName = "Matematyka", weight = 2.0), // 8.0
            Grade(id = "3", grade = "3+", subjectId = "1", subjectName = "Matematyka", weight = 1.0), // 3.5
            Grade(id = "4", grade = "np", subjectId = "1", subjectName = "Matematyka", weight = 1.0, countToAverage = false) // ignored
        )
        // total weight = 3 + 2 + 1 = 6
        // total sum = 15 + 8 + 3.5 = 26.5
        // average = 26.5 / 6 = 4.41666... -> 4.42
        val avg = GradeCalculator.calculateWeightedAverage(grades)
        assertNotNull(avg)
        assertEquals(4.42, avg)
    }

    @Test
    fun testGradeSimulator() {
        val grades = listOf(
            Grade(id = "1", grade = "4", subjectId = "1", subjectName = "Matematyka", weight = 2.0), // 8.0
            Grade(id = "2", grade = "4", subjectId = "1", subjectName = "Matematyka", weight = 2.0)  // 8.0
        )
        // current sum = 16, current weight = 4, avg = 4.0
        // simulate adding a 5 with weight 3 -> sum = 16 + 15 = 31, weight = 7 -> avg = 4.43
        val simulated = GradeCalculator.simulateNewAverage(grades, listOf(5.0 to 3.0))
        assertNotNull(simulated)
        assertEquals(4.43, simulated)
    }

    @Test
    fun testAttendanceCalculator() {
        val attendances = listOf(
            Attendance(id = "1", lessonNo = 1, date = "2026-10-14", subjectName = "Polski", type = AttendanceType.PRESENCE),
            Attendance(id = "2", lessonNo = 2, date = "2026-10-14", subjectName = "Matematyka", type = AttendanceType.PRESENCE),
            Attendance(id = "3", lessonNo = 3, date = "2026-10-14", subjectName = "Fizyka", type = AttendanceType.LATE),
            Attendance(id = "4", lessonNo = 4, date = "2026-10-14", subjectName = "Historia", type = AttendanceType.UNEXCUSED),
            Attendance(id = "5", lessonNo = 5, date = "2026-10-14", subjectName = "WF", type = AttendanceType.EXCUSED)
        )
        // total = 5, present = 2, late = 1 (present total = 3), unexcused = 1, excused = 1
        // presence pct = (3 / 5) * 100 = 60.0%
        val stats = AttendanceCalculator.calculateStats(attendances)
        assertEquals(5, stats.totalHours)
        assertEquals(2, stats.presentCount)
        assertEquals(1, stats.lateCount)
        assertEquals(1, stats.unexcusedCount)
        assertEquals(1, stats.excusedCount)
        assertEquals(60.0, stats.presencePercentage)
    }
}
