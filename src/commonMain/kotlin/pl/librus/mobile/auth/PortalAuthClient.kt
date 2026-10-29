package pl.librus.mobile.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pl.librus.mobile.domain.models.ChildAccount

class PortalAuthClient(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Extracts CSRF token from Portal login page HTML.
     */
    fun extractCsrfToken(html: String): String {
        // 1. Meta tag: <meta name="csrf-token" content="...">
        val metaPattern = Regex("""<meta\s+name=["']csrf-token["']\s+content=["']([^"']+)["']""")
        metaPattern.find(html)?.let { return it.groupValues[1] }

        // 2. Input tag: name="_token" value="..."
        val inputPattern = Regex("""name=["']_token["']\s+value=["']([^"']+)["']""")
        inputPattern.find(html)?.let { return it.groupValues[1] }

        // 3. Reversed input tag: value="..." name="_token"
        val altInputPattern = Regex("""value=["']([^"']+)["']\s+name=["']_token["']""")
        altInputPattern.find(html)?.let { return it.groupValues[1] }

        throw IllegalStateException("Nie znaleziono tokenu CSRF na stronie logowania Portal Rodzina.")
    }

    /**
     * Parses the SynergiaAccounts JSON array from portal.librus.pl/api/v3/SynergiaAccounts.
     */
    fun parseSynergiaAccounts(jsonString: String): List<ChildAccount> {
        val trimmed = jsonString.trim().removePrefix("\uFEFF")
        if (trimmed.isEmpty()) return emptyList()

        val root = try {
            json.parseToJsonElement(trimmed)
        } catch (_: Exception) {
            return emptyList()
        }

        val array = when {
            root is kotlinx.serialization.json.JsonArray -> root
            root is kotlinx.serialization.json.JsonObject && root.containsKey("accounts") -> root["accounts"]!!.jsonArray
            else -> emptyList()
        }

        return array.mapNotNull { element ->
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val accountId = obj["accountIdentifier"]?.jsonPrimitive?.content ?: id
            val studentName = obj["studentName"]?.jsonPrimitive?.content ?: "Uczeń"
            val login = obj["login"]?.jsonPrimitive?.content ?: accountId
            val token = obj["accessToken"]?.jsonPrimitive?.content ?: ""
            val state = obj["state"]?.jsonPrimitive?.content ?: "active"
            val group = obj["group"]?.jsonPrimitive?.content ?: "synergia"

            ChildAccount(
                id = id,
                accountIdentifier = accountId,
                studentName = studentName,
                login = login,
                accessToken = token,
                state = state,
                group = group
            )
        }
    }

    /**
     * Authenticates via Portal Rodzina and returns list of linked child accounts.
     */
    suspend fun login(email: String, pass: String): List<ChildAccount> {
        val loginPageUrl = "https://portal.librus.pl/konto-librus/login"
        val loginPageResp = httpClient.get(loginPageUrl) {
            header("User-Agent", USER_AGENT)
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        }
        val html = loginPageResp.bodyAsText()
        val csrf = extractCsrfToken(html)

        val actionUrl = "https://portal.librus.pl/konto-librus/login/action"
        val submitResp = httpClient.submitForm(
            url = actionUrl,
            formParameters = Parameters.build {
                append("_token", csrf)
                append("email", email.trim())
                append("password", pass)
            }
        ) {
            header("User-Agent", USER_AGENT)
            header("Referer", loginPageUrl)
            header("Origin", "https://portal.librus.pl")
        }

        val submitBody = submitResp.bodyAsText()
        if (submitResp.status.value in 400..599 || submitBody.contains("Błędny login lub hasło", ignoreCase = true)) {
            throw IllegalStateException("Nieprawidłowy e-mail lub hasło do konta Portal Rodzina.")
        }

        // Fetch linked Synergia child accounts
        val accountsResp = httpClient.get("https://portal.librus.pl/api/v3/SynergiaAccounts") {
            header("User-Agent", USER_AGENT)
            header("Accept", "application/json")
        }

        if (!accountsResp.status.isSuccess()) {
            throw IllegalStateException("Nie udało się pobrać listy uczniów powiązanych z kontem rodzica.")
        }

        val accountsJson = accountsResp.bodyAsText()
        val children = parseSynergiaAccounts(accountsJson)

        if (children.isEmpty()) {
            throw IllegalStateException("Brak powiązanych kont uczniów w Twoim koncie Portal Rodzina.")
        }

        return children
    }

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    }
}
