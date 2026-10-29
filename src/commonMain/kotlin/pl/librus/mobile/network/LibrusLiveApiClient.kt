package pl.librus.mobile.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.*
import pl.librus.mobile.auth.AuthBackendType
import pl.librus.mobile.auth.UserSession
import pl.librus.mobile.domain.models.*

class LibrusLiveApiClient(
    private val httpClient: HttpClient
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun baseUrl(backendType: AuthBackendType): String {
        return when (backendType) {
            AuthBackendType.PORTAL_RODZINA -> "https://api.librus.pl/3.0"
            AuthBackendType.GATEWAY_API_20 -> "https://synergia.librus.pl/gateway/api/2.0"
            AuthBackendType.DEMO_MOCK -> ""
        }
    }

    private suspend fun fetchJson(session: UserSession, path: String): JsonElement? {
        val base = baseUrl(session.backendType)
        if (base.isEmpty()) return null

        val fullUrl = "$base/$path"
        return try {
            val resp = httpClient.get(fullUrl) {
                if (session.backendType == AuthBackendType.PORTAL_RODZINA) {
                    header("Authorization", "Bearer ${session.activeChild.accessToken}")
                }
                header("Accept", "application/json")
                header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            }
            if (resp.status.isSuccess()) {
                val text = resp.bodyAsText().trim().removePrefix("\uFEFF")
                if (text.isNotEmpty()) json.parseToJsonElement(text) else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchMe(session: UserSession): StudentProfile? {
        val root = fetchJson(session, "Me")?.jsonObject ?: return null
        val me = root["Me"]?.jsonObject ?: return null
        val user = me["User"]?.jsonObject
        val firstName = user?.get("FirstName")?.jsonPrimitive?.contentOrNull ?: ""
        val lastName = user?.get("LastName")?.jsonPrimitive?.contentOrNull ?: ""
        val classObj = me["Class"]?.jsonObject
        val className = classObj?.get("Name")?.jsonPrimitive?.contentOrNull ?: ""
        val regNo = user?.get("ClassRegisterNumber")?.jsonPrimitive?.contentOrNull?.toIntOrNull()

        return StudentProfile(
            id = session.activeChild.id,
            firstName = firstName.ifEmpty { session.activeChild.studentName.split(" ").firstOrNull() ?: "" },
            lastName = lastName.ifEmpty { session.activeChild.studentName.split(" ").drop(1).joinToString(" ") },
            fullName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) "$firstName $lastName".trim() else session.activeChild.studentName,
            className = className,
            classRegisterNumber = regNo,
            accountEmail = session.identifier
        )
    }

    suspend fun fetchLuckyNumber(session: UserSession): LuckyNumber? {
        val root = fetchJson(session, "LuckyNumbers")?.jsonObject ?: return null
        val luckyObj = root["LuckyNumber"]?.jsonObject ?: return null
        val num = luckyObj["LuckyNumber"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return null
        val day = luckyObj["LuckyNumberDay"]?.jsonPrimitive?.contentOrNull ?: ""
        return LuckyNumber(number = num, date = day)
    }

    suspend fun fetchSubjects(session: UserSession): List<Subject> {
        val root = fetchJson(session, "Subjects")?.jsonObject ?: return emptyList()
        val array = root["Subjects"]?.jsonArray ?: return emptyList()

        return array.mapNotNull { elem ->
            val obj = elem.jsonObject
            val id = obj["Id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = obj["Name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val short = obj["Short"]?.jsonPrimitive?.contentOrNull ?: ""
            Subject(id = id, name = name, shortName = short)
        }
    }

    suspend fun fetchGradeCategories(session: UserSession): List<GradeCategory> {
        val root = fetchJson(session, "GradeCategories")?.jsonObject ?: return emptyList()
        val array = root["GradeCategories"]?.jsonArray ?: return emptyList()

        return array.mapNotNull { elem ->
            val obj = elem.jsonObject
            val id = obj["Id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = obj["Name"]?.jsonPrimitive?.contentOrNull ?: ""
            val weight = obj["Weight"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 1.0
            val colorObj = obj["Color"]?.jsonObject
            val rgb = colorObj?.get("RGB")?.jsonPrimitive?.contentOrNull ?: "7E57C2"
            val count = obj["CountToAverage"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: true

            GradeCategory(
                id = id,
                name = name,
                weight = weight,
                colorHex = "#$rgb",
                countToAverage = count
            )
        }
    }

    suspend fun fetchGrades(
        session: UserSession,
        subjectsMap: Map<String, Subject>,
        categoriesMap: Map<String, GradeCategory>
    ): List<Grade> {
        val root = fetchJson(session, "Grades")?.jsonObject ?: return emptyList()
        val array = root["Grades"]?.jsonArray ?: return emptyList()

        return array.mapNotNull { elem ->
            val obj = elem.jsonObject
            val id = obj["Id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val gradeVal = obj["Grade"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null

            val subjId = obj["Subject"]?.jsonObject?.get("Id")?.jsonPrimitive?.contentOrNull ?: ""
            val catId = obj["Category"]?.jsonObject?.get("Id")?.jsonPrimitive?.contentOrNull ?: ""

            val subject = subjectsMap[subjId]
            val category = categoriesMap[catId]

            val semester = obj["Semester"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
            val date = obj["Date"]?.jsonPrimitive?.contentOrNull ?: ""
            val addDate = obj["AddDate"]?.jsonPrimitive?.contentOrNull ?: ""

            val isConst = obj["IsConstituent"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: true
            val isSemProp = obj["IsSemesterProposal"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
            val isSem = obj["IsSemester"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
            val isFinProp = obj["IsFinalProposal"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
            val isFin = obj["IsFinal"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false

            Grade(
                id = id,
                grade = gradeVal,
                subjectId = subjId,
                subjectName = subject?.name ?: "Przedmiot $subjId",
                categoryId = catId,
                categoryName = category?.name ?: "",
                weight = category?.weight ?: 1.0,
                colorHex = category?.colorHex ?: "#7E57C2",
                countToAverage = category?.countToAverage ?: true,
                semester = semester,
                date = date,
                addDate = addDate,
                isConstituent = isConst,
                isSemesterProposal = isSemProp,
                isSemester = isSem,
                isFinalProposal = isFinProp,
                isFinal = isFin
            )
        }
    }
}
