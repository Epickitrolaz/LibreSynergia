package pl.librus.mobile.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class TimetableSlot(
    val id: String,
    val lessonNo: Int,
    val date: String,
    val subjectName: String,
    val teacherName: String = "",
    val classroom: String = "",
    val hourFrom: String = "",
    val hourTo: String = "",
    val isCancelled: Boolean = false,
    val isSubstitution: Boolean = false,
    val orgSubjectName: String? = null,
    val orgTeacherName: String? = null,
    val substitutionNote: String? = null,
    val groupName: String? = null
) {
    val timeRange: String
        get() = if (hourFrom.isNotEmpty() && hourTo.isNotEmpty()) "$hourFrom - $hourTo" else ""
}

@Serializable
data class TimetableDay(
    val date: String,
    val dayOfWeekName: String,
    val lessons: List<TimetableSlot> = emptyList()
)
