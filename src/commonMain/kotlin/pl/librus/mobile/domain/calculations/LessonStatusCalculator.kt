package pl.librus.mobile.domain.calculations

import kotlinx.datetime.LocalTime
import pl.librus.mobile.domain.models.TimetableSlot

sealed class LessonStatus {
    data class BeforeSchool(
        val firstLesson: TimetableSlot,
        val minutesToStart: Int
    ) : LessonStatus()

    data class LessonOngoing(
        val lesson: TimetableSlot,
        val minutesToEnd: Int,
        val progressFraction: Float,
        val nextLesson: TimetableSlot? = null
    ) : LessonStatus()

    data class BreakOngoing(
        val previousLesson: TimetableSlot,
        val nextLesson: TimetableSlot,
        val minutesToNextLesson: Int,
        val isNextCancelled: Boolean = false,
        val nextEffectiveLesson: TimetableSlot? = null
    ) : LessonStatus()

    data object SchoolFinished : LessonStatus()

    data object NoLessonsToday : LessonStatus()
}

object LessonStatusCalculator {

    fun parseTimeToMinutes(timeStr: String): Int? {
        val parts = timeStr.trim().split(":")
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    fun calculateStatus(
        todayLessons: List<TimetableSlot>,
        now: LocalTime
    ): LessonStatus {
        if (todayLessons.isEmpty()) {
            return LessonStatus.NoLessonsToday
        }

        val sortedLessons = todayLessons.sortedWith(
            compareBy<TimetableSlot> { parseTimeToMinutes(it.hourFrom) ?: (it.lessonNo * 60) }
                .thenBy { it.lessonNo }
        )

        val nowMinutes = now.hour * 60 + now.minute

        val firstLesson = sortedLessons.first()
        val firstStart = parseTimeToMinutes(firstLesson.hourFrom) ?: 0

        val lastLesson = sortedLessons.last()
        val lastEnd = parseTimeToMinutes(lastLesson.hourTo) ?: (firstStart + 45)

        if (nowMinutes < firstStart) {
            val minutesToStart = (firstStart - nowMinutes).coerceAtLeast(0)
            return LessonStatus.BeforeSchool(firstLesson, minutesToStart)
        }

        if (nowMinutes >= lastEnd) {
            return LessonStatus.SchoolFinished
        }

        for (i in sortedLessons.indices) {
            val slot = sortedLessons[i]
            val start = parseTimeToMinutes(slot.hourFrom) ?: continue
            val end = parseTimeToMinutes(slot.hourTo) ?: (start + 45)

            if (nowMinutes in start until end) {
                val totalDuration = (end - start).coerceAtLeast(1)
                val elapsed = nowMinutes - start
                val fraction = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
                val minutesToEnd = (end - nowMinutes).coerceAtLeast(1)
                val nextLesson = sortedLessons.getOrNull(i + 1)
                return LessonStatus.LessonOngoing(
                    lesson = slot,
                    minutesToEnd = minutesToEnd,
                    progressFraction = fraction,
                    nextLesson = nextLesson
                )
            }
        }

        for (i in 0 until sortedLessons.size - 1) {
            val currentSlot = sortedLessons[i]
            val nextSlot = sortedLessons[i + 1]

            val currentEnd = parseTimeToMinutes(currentSlot.hourTo) ?: continue
            val nextStart = parseTimeToMinutes(nextSlot.hourFrom) ?: continue

            if (nowMinutes in currentEnd until nextStart) {
                val minutesToNext = (nextStart - nowMinutes).coerceAtLeast(1)
                val isNextCancelled = nextSlot.isCancelled
                val nextEffective = if (isNextCancelled) {
                    sortedLessons.drop(i + 1).firstOrNull { !it.isCancelled }
                } else null

                return LessonStatus.BreakOngoing(
                    previousLesson = currentSlot,
                    nextLesson = nextSlot,
                    minutesToNextLesson = minutesToNext,
                    isNextCancelled = isNextCancelled,
                    nextEffectiveLesson = nextEffective
                )
            }
        }

        return LessonStatus.SchoolFinished
    }
}
