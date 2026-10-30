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
    fun testGradeColorCodingRanges() {
        // 6 (Celujący) -> Blue
        assertEquals("#1E88E5", Grade.getGradeColorHex("6"))
        assertEquals("#1E88E5", Grade.getGradeColorHex("6-"))
        assertEquals("#1E88E5", Grade.getGradeColorHex("5.75"))

        // 5 (Bardzo dobry) -> Green
        assertEquals("#2E7D32", Grade.getGradeColorHex("5"))
        assertEquals("#2E7D32", Grade.getGradeColorHex("5+"))
        assertEquals("#2E7D32", Grade.getGradeColorHex("5-"))
        assertEquals("#2E7D32", Grade.getGradeColorHex("4.80"))

        // 4 (Dobry) -> Light Green
        assertEquals("#689F38", Grade.getGradeColorHex("4"))
        assertEquals("#689F38", Grade.getGradeColorHex("4+"))
        assertEquals("#689F38", Grade.getGradeColorHex("4-"))
        assertEquals("#689F38", Grade.getGradeColorHex("3.85"))

        // 3 (Dostateczny) -> Yellow
        assertEquals("#F57F17", Grade.getGradeColorHex("3"))
        assertEquals("#F57F17", Grade.getGradeColorHex("3+"))
        assertEquals("#F57F17", Grade.getGradeColorHex("3-"))
        assertEquals("#F57F17", Grade.getGradeColorHex("2.90"))

        // 2 (Dopuszczający) -> Orange
        assertEquals("#FB8C00", Grade.getGradeColorHex("2"))
        assertEquals("#FB8C00", Grade.getGradeColorHex("2+"))
        assertEquals("#FB8C00", Grade.getGradeColorHex("2-"))
        assertEquals("#FB8C00", Grade.getGradeColorHex("1.80"))

        // 1 (Niedostateczny) -> Red
        assertEquals("#D32F2F", Grade.getGradeColorHex("1"))
        assertEquals("#D32F2F", Grade.getGradeColorHex("1+"))
        assertEquals("#D32F2F", Grade.getGradeColorHex("1.20"))

        // Special symbols
        assertEquals("#2E7D32", Grade.getGradeColorHex("+"))
        assertEquals("#D32F2F", Grade.getGradeColorHex("-"))
        assertEquals("#757575", Grade.getGradeColorHex("np"))
        assertEquals("#757575", Grade.getGradeColorHex("bz"))
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

    @Test
    fun testLessonStatusCalculator() {
        val slots = listOf(
            pl.librus.mobile.domain.models.TimetableSlot(
                id = "s1",
                lessonNo = 1,
                date = "2026-10-30",
                subjectName = "Matematyka",
                classroom = "23",
                hourFrom = "08:00",
                hourTo = "08:45"
            ),
            pl.librus.mobile.domain.models.TimetableSlot(
                id = "s2",
                lessonNo = 2,
                date = "2026-10-30",
                subjectName = "Język polski",
                classroom = "104",
                hourFrom = "08:55",
                hourTo = "09:40",
                isCancelled = true
            ),
            pl.librus.mobile.domain.models.TimetableSlot(
                id = "s3",
                lessonNo = 3,
                date = "2026-10-30",
                subjectName = "Historia",
                classroom = "12",
                hourFrom = "09:50",
                hourTo = "10:35"
            )
        )

        // 07:30 -> BeforeSchool
        val before = pl.librus.mobile.domain.calculations.LessonStatusCalculator.calculateStatus(
            slots,
            kotlinx.datetime.LocalTime(7, 30)
        )
        kotlin.test.assertTrue(before is pl.librus.mobile.domain.calculations.LessonStatus.BeforeSchool)
        assertEquals(30, before.minutesToStart)

        // 08:20 -> LessonOngoing (Math)
        val during = pl.librus.mobile.domain.calculations.LessonStatusCalculator.calculateStatus(
            slots,
            kotlinx.datetime.LocalTime(8, 20)
        )
        kotlin.test.assertTrue(during is pl.librus.mobile.domain.calculations.LessonStatus.LessonOngoing)
        assertEquals("Matematyka", during.lesson.subjectName)
        assertEquals(25, during.minutesToEnd)

        // 08:50 -> BreakOngoing (Between s1 and s2, s2 is cancelled, next effective is s3)
        val breakStatus = pl.librus.mobile.domain.calculations.LessonStatusCalculator.calculateStatus(
            slots,
            kotlinx.datetime.LocalTime(8, 50)
        )
        kotlin.test.assertTrue(breakStatus is pl.librus.mobile.domain.calculations.LessonStatus.BreakOngoing)
        assertEquals(5, breakStatus.minutesToNextLesson)
        kotlin.test.assertTrue(breakStatus.isNextCancelled)
        assertEquals("Historia", breakStatus.nextEffectiveLesson?.subjectName)

        // 11:00 -> SchoolFinished
        val finished = pl.librus.mobile.domain.calculations.LessonStatusCalculator.calculateStatus(
            slots,
            kotlinx.datetime.LocalTime(11, 0)
        )
        kotlin.test.assertTrue(finished is pl.librus.mobile.domain.calculations.LessonStatus.SchoolFinished)
    }
}
