package pl.librus.mobile.network

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LibrusCookieStorage : CookiesStorage {

    private val mutex = Mutex()
    private val cookies = mutableListOf<Cookie>()

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        val host = requestUrl.host.lowercase()
        val path = requestUrl.encodedPath

        cookies.filter { cookie ->
            val cookieDomain = cookie.domain?.lowercase()?.removePrefix(".")
            val matchesDomain = cookieDomain == null || host == cookieDomain || host.endsWith(".$cookieDomain")

            // Critical rule from docs/QUIRKS_AND_EDGE_CASES.md:
            // DeviceCookie is set with Path=/OAuth but must be sent on all subsequent requests to api.librus.pl
            val isDeviceCookie = cookie.name.equals("DeviceCookie", ignoreCase = true)
            val matchesPath = if (isDeviceCookie) {
                true
            } else {
                val cPath = cookie.path ?: "/"
                path.startsWith(cPath)
            }

            matchesDomain && matchesPath
        }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie): Unit = mutex.withLock {
        val domain = cookie.domain?.ifEmpty { null } ?: requestUrl.host
        val normalizedCookie = cookie.copy(domain = domain)

        cookies.removeAll { it.name == normalizedCookie.name && it.domain == normalizedCookie.domain }
        cookies.add(normalizedCookie)
    }

    override fun close() {
        cookies.clear()
    }

    suspend fun getAllCookies(): List<Cookie> = mutex.withLock {
        cookies.toList()
    }

    suspend fun clear() = mutex.withLock {
        cookies.clear()
    }
}
