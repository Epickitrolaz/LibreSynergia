package pl.librus.mobile.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.*
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
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /** Null means a failed sync; an empty week is a successful, authoritative response. */
    suspend fun fetchTimetable(
        session: UserSession,
        today: LocalDate = Clock.System.todayIn(TimeZone.of("Europe/Warsaw"))
    ): List<TimetableDay>? {
        val monday = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
        val root = fetchJson(session, "Timetables?weekStart=$monday") as? JsonObject ?: return null
        // Sequential requests keep concurrency below the per-host limit.
        val subjects = fetchSubjects(session).associate { it.id to it.name }
        val teachers = fetchNames(session, "Users") { user ->
            listOfNotNull(user.text("FirstName"), user.text("LastName")).joinToString(" ")
        }
        val classrooms = fetchNames(session, "Classrooms") { it.text("Name").orEmpty() }
        return try {
            TimetableParser.parse(root, monday, subjects, teachers, classrooms)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Null denotes a failed/incomplete sync, not an empty attendance register. */
    suspend fun fetchAttendances(session: UserSession): List<Attendance>? {
        val root = fetchJson(session, "Attendances") as? JsonObject ?: return null
        val entries = root["Attendances"] as? JsonArray ?: return null
        if (entries.isEmpty()) return emptyList()
        return try {
            val nativeTypes = fetchJson(session, "Attendances/Types") as? JsonObject
            val types = nativeTypes?.let { AttendanceParser.types(it) }
                ?: (fetchJson(session, "AttendanceTypes") as? JsonObject)?.let { AttendanceParser.types(it) }
                ?: return null
            val subjects = fetchNames(session, "Subjects") { it.text("Name").orEmpty() }
            val lessons = fetchNames(session, "Lessons") {
                (it["Subject"] as? JsonObject)?.text("Id").orEmpty()
            }
            AttendanceParser.parse(entries, types, subjects, lessons)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchNames(
        session: UserSession,
        endpoint: String,
        name: (JsonObject) -> String
    ): Map<String, String> {
        val root = fetchJson(session, endpoint) as? JsonObject ?: return emptyMap()
        return (root[endpoint] as? JsonArray).orEmpty().mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val id = item.text("Id") ?: return@mapNotNull null
            id to name(item)
        }.toMap()
    }

    private fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull

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

    suspend fun fetchSchoolNotices(session: UserSession): List<SchoolNotice>? {
        val root = fetchJson(session, "SchoolNotices")?.jsonObject ?: return null
        val array = root["SchoolNotices"]?.jsonArray ?: return emptyList()

        return array.mapNotNull { elem ->
            val obj = elem.jsonObject
            val id = obj["Id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val subject = obj["Subject"]?.jsonPrimitive?.contentOrNull ?: ""
            val content = obj["Content"]?.jsonPrimitive?.contentOrNull ?: ""
            val startDate = obj["StartDate"]?.jsonPrimitive?.contentOrNull ?: ""
            val creationDate = obj["CreationDate"]?.jsonPrimitive?.contentOrNull ?: startDate
            val wasRead = obj["WasRead"]?.jsonPrimitive?.booleanOrNull ?: false

            SchoolNotice(
                id = id,
                subject = subject,
                content = content,
                author = "",
                date = creationDate,
                isRead = wasRead
            )
        }
    }

    suspend fun markNoticeAsRead(session: UserSession, noticeId: String) {
        val base = baseUrl(session.backendType)
        if (base.isEmpty()) return
        try {
            httpClient.get("$base/SchoolNotices/MarkAsRead/$noticeId") {
                if (session.backendType == AuthBackendType.PORTAL_RODZINA) {
                    header("Authorization", "Bearer ${session.activeChild.accessToken}")
                }
                header("Accept", "application/json")
                header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
            }
        } catch (_: Exception) {}
    }

    suspend fun fetchMessages(session: UserSession, usersMap: Map<String, String>): List<LibrusMessage>? {
        val root = fetchJson(session, "Messages")?.jsonObject ?: return null
        val array = (root["Messages"] as? JsonArray) ?: (root["data"] as? JsonArray) ?: return emptyList()

        return array.mapNotNull { elem ->
            val obj = elem.jsonObject
            val id = obj["Id"]?.jsonPrimitive?.contentOrNull
                ?: obj["messageId"]?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null

            val subject = obj["Subject"]?.jsonPrimitive?.contentOrNull
                ?: obj["topic"]?.jsonPrimitive?.contentOrNull
                ?: "(Bez tematu)"

            val body = obj["Body"]?.jsonPrimitive?.contentOrNull
                ?: obj["content"]?.jsonPrimitive?.contentOrNull
                ?: ""

            val senderObj = obj["Sender"] as? JsonObject
            val senderId = senderObj?.get("Id")?.jsonPrimitive?.contentOrNull
                ?: obj["senderId"]?.jsonPrimitive?.contentOrNull

            val senderNameFromObj = listOfNotNull(
                senderObj?.get("FirstName")?.jsonPrimitive?.contentOrNull,
                senderObj?.get("LastName")?.jsonPrimitive?.contentOrNull
            ).filter { it.isNotBlank() }.joinToString(" ")

            val senderName = when {
                senderNameFromObj.isNotBlank() -> senderNameFromObj
                senderId != null && usersMap.containsKey(senderId) -> usersMap[senderId].orEmpty()
                else -> obj["senderName"]?.jsonPrimitive?.contentOrNull ?: "Nauczyciel"
            }

            val date = obj["SendDate"]?.jsonPrimitive?.contentOrNull
                ?: obj["sendDate"]?.jsonPrimitive?.contentOrNull
                ?: obj["Date"]?.jsonPrimitive?.contentOrNull
                ?: ""

            val readDate = obj["ReadDate"]?.jsonPrimitive?.contentOrNull
                ?: obj["readDate"]?.jsonPrimitive?.contentOrNull
            val wasRead = (readDate != null && readDate.isNotBlank()) ||
                (obj["WasRead"]?.jsonPrimitive?.booleanOrNull == true)

            val attachmentsArray = (obj["Attachments"] as? JsonArray) ?: (obj["attachments"] as? JsonArray)
            val attachmentsList = attachmentsArray?.mapNotNull { attElem ->
                val attObj = attElem as? JsonObject ?: return@mapNotNull null
                val attId = attObj["Id"]?.jsonPrimitive?.contentOrNull ?: attObj["id"]?.jsonPrimitive?.contentOrNull ?: ""
                val attName = attObj["Name"]?.jsonPrimitive?.contentOrNull ?: attObj["name"]?.jsonPrimitive?.contentOrNull ?: "Załącznik"
                MessageAttachment(id = attId, name = attName)
            }.orEmpty()

            LibrusMessage(
                id = id,
                subject = subject,
                body = body,
                senderName = senderName,
                senderId = senderId,
                date = date,
                isRead = wasRead,
                hasAttachments = attachmentsList.isNotEmpty() || obj["HasAttachments"]?.jsonPrimitive?.booleanOrNull == true,
                attachments = attachmentsList
            )
        }
    }

    suspend fun fetchHomeWorksAndAgenda(
        session: UserSession,
        subjectsMap: Map<String, Subject>,
        usersMap: Map<String, String>
    ): Pair<List<Homework>, List<AgendaEvent>>? {
        val root = fetchJson(session, "HomeWorks")?.jsonObject ?: return null
        val array = root["HomeWorks"]?.jsonArray ?: return emptyList<Homework>() to emptyList<AgendaEvent>()

        // Fetch categories to get category names if possible
        val categoriesRoot = fetchJson(session, "HomeWorks/Categories")?.jsonObject
        val categoriesArray = categoriesRoot?.get("Categories")?.jsonArray
        val categoryNames = categoriesArray?.mapNotNull { catElem ->
            val catObj = catElem as? JsonObject ?: return@mapNotNull null
            val id = catObj["Id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = catObj["Name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            id to name
        }?.toMap().orEmpty()

        val homeworks = mutableListOf<Homework>()
        val agendaEvents = mutableListOf<AgendaEvent>()

        for (elem in array) {
            val obj = elem as? JsonObject ?: continue
            val id = obj["Id"]?.jsonPrimitive?.contentOrNull ?: continue
            val date = obj["Date"]?.jsonPrimitive?.contentOrNull ?: ""
            val content = obj["Content"]?.jsonPrimitive?.contentOrNull ?: ""
            val addDate = obj["AddDate"]?.jsonPrimitive?.contentOrNull ?: ""
            val timeFrom = obj["TimeFrom"]?.jsonPrimitive?.contentOrNull
            val timeTo = obj["TimeTo"]?.jsonPrimitive?.contentOrNull
            val lessonNo = obj["LessonNo"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

            val subjId = (obj["Subject"] as? JsonObject)?.get("Id")?.jsonPrimitive?.contentOrNull ?: ""
            val subj = subjectsMap[subjId]
            val subjName = subj?.name ?: if (subjId.isNotEmpty()) "Przedmiot $subjId" else "Inne"

            val teacherId = (obj["CreatedBy"] as? JsonObject)?.get("Id")?.jsonPrimitive?.contentOrNull ?: ""
            val teacherName = usersMap[teacherId] ?: ""

            val catId = (obj["Category"] as? JsonObject)?.get("Id")?.jsonPrimitive?.contentOrNull ?: ""
            val catName = categoryNames[catId] ?: ""

            val lowerContent = content.lowercase()
            val lowerCat = catName.lowercase()

            val (eventType, typeName, colorHex) = when {
                lowerContent.contains("sprawdzian") || lowerCat.contains("sprawdzian") ||
                lowerContent.contains("praca klasowa") || lowerCat.contains("praca klasowa") -> {
                    Triple(AgendaEventType.EXAM, "Sprawdzian", "#F44336")
                }
                lowerContent.contains("kartkówka") || lowerCat.contains("kartkówka") -> {
                    Triple(AgendaEventType.QUIZ, "Kartkówka", "#FF9800")
                }
                lowerContent.contains("projekt") || lowerCat.contains("projekt") -> {
                    Triple(AgendaEventType.PROJECT, "Projekt", "#9C27B0")
                }
                lowerContent.contains("wycieczka") || lowerCat.contains("wycieczka") ||
                lowerContent.contains("wyjście") -> {
                    Triple(AgendaEventType.TRIP, "Wycieczka", "#4CAF50")
                }
                lowerContent.contains("zebranie") || lowerCat.contains("zebranie") ||
                lowerContent.contains("konsultacj") -> {
                    Triple(AgendaEventType.MEETING, "Zebranie", "#2196F3")
                }
                lowerContent.contains("zadanie") || lowerCat.contains("zadanie") ||
                lowerCat.contains("domow") || lowerContent.contains("ćwiczen") -> {
                    Triple(AgendaEventType.HOMEWORK, "Zadanie domowe", "#3F51B5")
                }
                else -> {
                    Triple(AgendaEventType.OTHER, if (catName.isNotEmpty()) catName else "Wydarzenie", "#607D8B")
                }
            }

            val title = if (content.length > 50) {
                content.substringBefore("\n").take(50).trim() + "…"
            } else {
                content.ifEmpty { typeName }
            }

            agendaEvents.add(
                AgendaEvent(
                    id = id,
                    date = date,
                    timeFrom = timeFrom,
                    timeTo = timeTo,
                    lessonNo = lessonNo,
                    title = title,
                    description = content,
                    subjectId = subjId,
                    subjectName = subjName,
                    teacherName = teacherName,
                    type = eventType,
                    typeName = typeName,
                    colorHex = colorHex
                )
            )

            // If it's a homework assignment or homework category, also add to homework list
            if (eventType == AgendaEventType.HOMEWORK || lowerCat.contains("domow") || lowerCat.contains("zadanie")) {
                homeworks.add(
                    Homework(
                        id = id,
                        date = date,
                        addDate = addDate,
                        subjectId = subjId,
                        subjectName = subjName,
                        teacherName = teacherName,
                        content = content,
                        categoryName = if (catName.isNotEmpty()) catName else "Zadanie domowe",
                        isDone = false
                    )
                )
            }
        }

        // Also fetch school free days into Agenda
        try {
            val freeDaysRoot = fetchJson(session, "Calendars/SchoolFreeDays")?.jsonObject
            val freeDaysArray = freeDaysRoot?.get("SchoolFreeDays")?.jsonArray
            freeDaysArray?.forEach { fdElem ->
                val fdObj = fdElem as? JsonObject ?: return@forEach
                val fdId = fdObj["Id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val dateFrom = fdObj["DateFrom"]?.jsonPrimitive?.contentOrNull ?: ""
                val name = fdObj["Name"]?.jsonPrimitive?.contentOrNull ?: "Dzień wolny"
                agendaEvents.add(
                    AgendaEvent(
                        id = "free_$fdId",
                        date = dateFrom,
                        title = name,
                        description = "Dzień wolny od zajęć dydaktycznych",
                        subjectName = "Szkoła",
                        type = AgendaEventType.TRIP,
                        typeName = "Dzień wolny",
                        colorHex = "#4CAF50"
                    )
                )
            }
        } catch (_: Exception) {}

        return homeworks to agendaEvents
    }
}
