package pl.librus.mobile.network

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.*
import pl.librus.mobile.domain.models.*

/** API IDs are opaque: native standard types do not use the demo's numbering. */
internal object AttendanceParser {
    fun types(root: JsonObject): Map<String, AttendanceType>? {
        val native = root["Types"] as? JsonArray
        val entries = native ?: root["AttendanceTypes"] as? JsonArray ?: return null
        return entries.associate { element ->
            val item = element as? JsonObject ?: error("Invalid attendance type")
            val id = requireNotNull(item.text("Id"))
            val standard = item.reference("StandardType") ?: id
            val kind = if (native != null) {
                when (standard) {
                    "1" -> AttendanceKind.UNEXCUSED
                    "2" -> AttendanceKind.LATE
                    "3" -> AttendanceKind.EXCUSED
                    "4" -> AttendanceKind.EXEMPT
                    else -> AttendanceKind.PRESENT
                }
            } else {
                when {
                    item.flag("IsAbsence") -> if (standard == "3") AttendanceKind.EXCUSED else AttendanceKind.UNEXCUSED
                    item.flag("IsPresence") -> if (standard == "4") AttendanceKind.LATE else AttendanceKind.PRESENT
                    else -> AttendanceKind.EXEMPT
                }
            }
            val color = item.text("ColorRGB")?.removePrefix("#")
            val defaultColor = when (kind) {
                AttendanceKind.UNEXCUSED -> "E53935"
                AttendanceKind.EXCUSED -> "1E88E5"
                AttendanceKind.LATE -> "FB8C00"
                AttendanceKind.EXEMPT, AttendanceKind.UNKNOWN -> "757575"
                AttendanceKind.PRESENT -> "4CAF50"
            }
            id to AttendanceType(
                id, item.text("Name").orEmpty(), item.text("Short").orEmpty(),
                kind == AttendanceKind.PRESENT || kind == AttendanceKind.LATE,
                kind == AttendanceKind.UNEXCUSED || kind == AttendanceKind.EXCUSED,
                "#${color?.takeIf { it.matches(Regex("[0-9a-fA-F]{6}")) } ?: defaultColor}", kind
            )
        }
    }

    fun parse(
        entries: JsonArray,
        types: Map<String, AttendanceType>,
        subjects: Map<String, String>,
        lessons: Map<String, String>
    ): List<Attendance> = entries.map { element ->
        val item = element as? JsonObject ?: error("Invalid attendance")
        val date = requireNotNull(item.text("Date"))
        LocalDate.parse(date)
        val subjectId = item.reference("Subject") ?: lessons[item.reference("Lesson")]
        Attendance(
            id = requireNotNull(item.text("Id")),
            lessonNo = requireNotNull(item.text("LessonNo")?.toIntOrNull()),
            date = date,
            subjectName = subjects[subjectId].orEmpty(),
            type = requireNotNull(types[item.reference("Type")])
        )
    }.distinctBy { it.id }.sortedWith(compareByDescending<Attendance> { it.date }.thenBy { it.lessonNo })

    private fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.reference(key: String) = (get(key) as? JsonObject)?.text("Id")
    private fun JsonObject.flag(key: String) = text(key) in listOf("true", "1")
}
