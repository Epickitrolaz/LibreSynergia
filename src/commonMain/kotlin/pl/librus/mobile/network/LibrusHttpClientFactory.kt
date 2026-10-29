package pl.librus.mobile.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object LibrusHttpClientFactory {

    val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun createClient(cookieStorage: LibrusCookieStorage = LibrusCookieStorage()): HttpClient {
        return HttpClient {
            install(HttpCookies) {
                storage = cookieStorage
            }
            install(ContentNegotiation) {
                json(jsonConfig)
            }
            followRedirects = true
        }
    }
}
