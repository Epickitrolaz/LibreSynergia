package pl.librus.mobile.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class StudentProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val fullName: String = "$firstName $lastName",
    val className: String = "",
    val classRegisterNumber: Int? = null,
    val schoolName: String = "",
    val isParentView: Boolean = false,
    val accountEmail: String? = null
)

@Serializable
data class ChildAccount(
    val id: String,
    val accountIdentifier: String,
    val studentName: String,
    val login: String,
    val accessToken: String,
    val state: String = "active",
    val group: String = "synergia"
)

@Serializable
data class ClassInfo(
    val id: String,
    val number: Int = 0,
    val symbol: String = "",
    val name: String = "$number$symbol"
)
