package pl.librus.mobile.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pl.librus.mobile.domain.models.ChildAccount
import kotlin.random.Random

class GatewayAuthClient(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    data class GatewaySession(
        val login: String,
        val cookies: Map<String, String>,
        val childAccount: ChildAccount,
        val className: String = ""
    )

    suspend fun login(login: String, pass: String): GatewaySession {
        val trimmedLogin = login.trim()
        val authBaseUrl = "https://api.librus.pl"
        val synergiaBaseUrl = "https://synergia.librus.pl"

        // Step 1: Initialize session starting at portalRodzina
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val initUrl = "$synergiaBaseUrl/loguj/portalRodzina?v=$timestamp"
        try {
            httpClient.get(initUrl) {
                header("User-Agent", USER_AGENT)
                header("Referer", "https://portal.librus.pl/")
            }
        } catch (_: Exception) {
            // Ignore redirect follow errors if any, cookies are stored in jar
        }

        // Step 2: Ensure OAuth Authorization page is loaded to establish session cookies
        val authPageUrl = "$authBaseUrl/OAuth/Authorization?client_id=46"
        try {
            httpClient.get(authPageUrl) {
                header("User-Agent", USER_AGENT)
                header("Referer", "https://portal.librus.pl/")
            }
        } catch (_: Exception) {}

        // Step 3: Post credentials with anti-bot X-Baner header
        val baner = createBanerHeader()
        val postResp = httpClient.submitForm(
            url = authPageUrl,
            formParameters = Parameters.build {
                append("action", "login")
                append("login", trimmedLogin)
                append("pass", pass)
            }
        ) {
            header("Accept", "application/json, text/javascript, */*; q=0.01")
            header("X-Requested-With", "XMLHttpRequest")
            header("X-Baner", baner)
            header("Referer", authPageUrl)
            header("Origin", authBaseUrl)
            header("User-Agent", USER_AGENT)
        }

        val postStatus = postResp.status.value
        val postText = postResp.bodyAsText().trim().removePrefix("\uFEFF")

        // Step 4: Validate server response
        if (postStatus in 400..599 || postText.contains("\"status\":\"error\"") || postText.contains("\"status\": \"error\"")) {
            val errorMsg = extractErrorMessage(postText, "Nieprawidłowy login lub hasło do systemu Librus.")
            throw IllegalStateException(errorMsg)
        }

        if (postText.contains("captcha", ignoreCase = true) || postText.contains("recaptcha", ignoreCase = true)) {
            throw IllegalStateException("Wymagana weryfikacja CAPTCHA. Zaloguj się przez przeglądarkę na synergia.librus.pl")
        }

        // Check goTo continuation
        var goTo: String? = null
        try {
            val root = json.parseToJsonElement(postText)
            if (root is JsonObject) {
                goTo = root["goTo"]?.jsonPrimitive?.contentOrNull
            }
        } catch (_: Exception) {}

        if (goTo != null && goTo.isNotEmpty()) {
            val redirectUrl = if (goTo.startsWith("http")) goTo else "$authBaseUrl$goTo"
            try {
                httpClient.get(redirectUrl) {
                    header("User-Agent", USER_AGENT)
                    header("Referer", authPageUrl)
                }
            } catch (_: Exception) {}
        }

        // Step 5: Follow standard Gateway completion sequence (PerformLogin & Grant)
        val performLoginUrl = "$authBaseUrl/OAuth/Authorization/PerformLogin?client_id=46"
        try {
            httpClient.get(performLoginUrl) {
                header("User-Agent", USER_AGENT)
                header("Referer", authPageUrl)
            }
            httpClient.get(performLoginUrl) {
                header("User-Agent", USER_AGENT)
                header("Referer", authPageUrl)
            }
        } catch (_: Exception) {}

        val grantUrl = "$authBaseUrl/OAuth/Authorization/Grant?client_id=46"
        try {
            httpClient.get(grantUrl) {
                header("User-Agent", USER_AGENT)
                header("Referer", authPageUrl)
            }
        } catch (_: Exception) {}

        // Step 6: Verify login via /Me or /Auth/TokenInfo
        val meUrl = "$synergiaBaseUrl/gateway/api/2.0/Me"
        var studentName = trimmedLogin
        var studentId = trimmedLogin
        var className = ""

        try {
            val meResp = httpClient.get(meUrl) {
                header("User-Agent", USER_AGENT)
                header("Accept", "application/json")
            }
            if (meResp.status.isSuccess()) {
                val meText = meResp.bodyAsText().trim().removePrefix("\uFEFF")
                val root = json.parseToJsonElement(meText).jsonObject
                val meObj = root["Me"]?.jsonObject
                val userObj = meObj?.get("User")?.jsonObject
                val firstName = userObj?.get("FirstName")?.jsonPrimitive?.contentOrNull ?: ""
                val lastName = userObj?.get("LastName")?.jsonPrimitive?.contentOrNull ?: ""
                if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                    studentName = "$firstName $lastName".trim()
                }
                studentId = userObj?.get("Id")?.jsonPrimitive?.contentOrNull ?: trimmedLogin
                className = meObj?.get("Class")?.jsonObject?.get("Name")?.jsonPrimitive?.contentOrNull ?: ""
            } else if (meResp.status.value == 401 || meResp.status.value == 403) {
                throw IllegalStateException("Nie udało się potwierdzić sesji logowania w Synergia Gateway.")
            }
        } catch (e: Exception) {
            if (e is IllegalStateException) throw e
        }

        val child = ChildAccount(
            id = studentId,
            accountIdentifier = trimmedLogin,
            studentName = studentName,
            login = trimmedLogin,
            accessToken = "gateway_session",
            state = "active"
        )

        return GatewaySession(
            login = trimmedLogin,
            cookies = emptyMap(),
            childAccount = child,
            className = className
        )
    }

    private fun extractErrorMessage(bodyText: String, defaultMessage: String): String {
        val trimmed = bodyText.trim().removePrefix("\uFEFF")
        if (trimmed.isEmpty()) return defaultMessage

        try {
            val root = json.parseToJsonElement(trimmed)
            if (root is JsonObject) {
                val errors = root["errors"]
                if (errors is JsonArray && errors.isNotEmpty()) {
                    val firstErr = errors[0]
                    if (firstErr is JsonObject) {
                        val msg = firstErr["message"]?.jsonPrimitive?.contentOrNull
                        if (!msg.isNullOrBlank()) return msg
                    }
                }
                val message = root["message"]?.jsonPrimitive?.contentOrNull
                if (!message.isNullOrBlank()) return message

                val errorDesc = root["error_description"]?.jsonPrimitive?.contentOrNull
                if (!errorDesc.isNullOrBlank()) return errorDesc
            }
        } catch (_: Exception) {}

        return defaultMessage
    }

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

        fun createBanerHeader(): String {
            val timestampPart = encodeBanerSegment(Clock.System.now().toEpochMilliseconds().toString())
            val randomPart = encodeBanerSegment(Random.nextDouble().toString())
            return "${randomPart}_${timestampPart}"
        }

        fun encodeBanerSegment(value: String): String {
            return value.map { (it.code + 20).toChar() }.joinToString("")
        }
    }
}
