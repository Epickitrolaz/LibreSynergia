package pl.librus.mobile.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class AgendaEventType {
    EXAM,        // Sprawdzian
    QUIZ,        // Kartkówka
    HOMEWORK,    // Zadanie domowe
    PROJECT,     // Projekt
    TRIP,        // Wycieczka
    MEETING,     // Zebranie
    INFORMATION, // Informacja
    OTHER        // Inne
}

@Serializable
data class AgendaEvent(
    val id: String,
    val date: String, // "YYYY-MM-DD"
    val timeFrom: String? = null,
    val timeTo: String? = null,
    val lessonNo: Int? = null,
    val title: String,
    val description: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherName: String = "",
    val type: AgendaEventType = AgendaEventType.OTHER,
    val typeName: String = "",
    val colorHex: String = "#F44336"
)
