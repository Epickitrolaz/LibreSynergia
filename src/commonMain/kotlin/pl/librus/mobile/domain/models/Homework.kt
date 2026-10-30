package pl.librus.mobile.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Homework(
    val id: String,
    val date: String, // Due date: "YYYY-MM-DD"
    val addDate: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val teacherName: String = "",
    val content: String = "",
    val categoryName: String = "Zadanie domowe",
    val isDone: Boolean = false,
    val attachments: List<HomeworkAttachment> = emptyList()
)

@Serializable
data class HomeworkAttachment(
    val id: String,
    val name: String
)
