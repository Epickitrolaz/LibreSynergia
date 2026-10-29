package pl.librus.mobile.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class SchoolNotice(
    val id: String,
    val subject: String,
    val content: String,
    val author: String,
    val date: String,
    val isRead: Boolean = true
)

@Serializable
data class LuckyNumber(
    val number: Int,
    val date: String
)
