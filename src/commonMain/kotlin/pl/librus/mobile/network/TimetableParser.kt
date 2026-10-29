package pl.librus.mobile.network

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.json.*
import pl.librus.mobile.domain.models.TimetableDay
import pl.librus.mobile.domain.models.TimetableSlot

/** Resolves API references from the lookup tables fetched with the timetable. */
internal object TimetableParser {
    fun parse(
        root: JsonObject,
        weekStart: LocalDate,
        subjects: Map<String, String>,
        teachers: Map<String, String>,
        classrooms: Map<String, String>
    ): List<TimetableDay>? {
        val days = root["Timetable"] as? JsonObject ?: return null
        return (0..6).map { offset ->
            val date = weekStart.plus(offset, DateTimeUnit.DAY)
            val periods = days[date.toString()]
            // Empty days may be encoded as [] instead of {}.
            require(periods == null || periods is JsonObject || periods == JsonArray(emptyList()))
            val lessons = (periods as? JsonObject).orEmpty().flatMap { (period, entries) ->
                require(entries is JsonArray)
                entries.mapIndexedNotNull { index, element ->
                    val slot = element as? JsonObject ?: return@mapIndexedNotNull null
                    val lessonNo = slot.text("LessonNo")?.toIntOrNull() ?: period.toIntOrNull()
                        ?: return@mapIndexedNotNull null
                    TimetableSlot(
                        id = "$date:$lessonNo:${slot.reference("TimetableEntry") ?: slot.text("Id").orEmpty()}:$index",
                        lessonNo = lessonNo,
                        date = date.toString(),
                        subjectName = resolve(slot, "Subject", subjects) ?: "—",
                        teacherName = resolve(slot, "Teacher", teachers).orEmpty(),
                        classroom = resolve(slot, "Classroom", classrooms).orEmpty(),
                        hourFrom = slot.text("HourFrom").orEmpty(),
                        hourTo = slot.text("HourTo").orEmpty(),
                        isCancelled = slot.flag("IsCancelled"),
                        isSubstitution = slot.flag("IsSubstitution"),
                        orgSubjectName = resolve(slot, "OrgSubject", subjects),
                        orgTeacherName = resolve(slot, "OrgTeacher", teachers),
                        substitutionNote = slot.text("SubstitutionNote"),
                        groupName = (slot["Group"] as? JsonObject)?.text("Name") ?: slot.text("Group")
                    )
                }
            }.sortedWith(compareBy({ it.lessonNo }, { it.hourFrom }))
            TimetableDay(date.toString(), date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }, lessons)
        }
    }

    private fun resolve(slot: JsonObject, key: String, names: Map<String, String>): String? =
        (slot[key] as? JsonObject)?.text("Name") ?: names[slot.reference(key)]

    private fun JsonObject.reference(key: String): String? = (get(key) as? JsonObject)?.text("Id")
    private fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.flag(key: String): Boolean = text(key).let { it == "true" || it == "1" }
}
