package pl.librus.mobile

import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import pl.librus.mobile.network.LibrusCookieStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CookieStorageTest {

    @Test
    fun testDeviceCookieAlwaysSentToApiLibrus() = runBlocking {
        val storage = LibrusCookieStorage()
        storage.addCookie(
            Url("https://api.librus.pl/OAuth/Authorization"),
            Cookie(name = "DeviceCookie", value = "secret_device_123", domain = "api.librus.pl", path = "/OAuth")
        )
        storage.addCookie(
            Url("https://synergia.librus.pl/"),
            Cookie(name = "oauth_token", value = "token_abc", domain = "synergia.librus.pl", path = "/")
        )

        // Request to root / on api.librus.pl should still receive DeviceCookie due to the path trap bypass
        val apiCookies = storage.get(Url("https://api.librus.pl/"))
        assertEquals(1, apiCookies.size)
        assertEquals("DeviceCookie", apiCookies[0].name)
        assertEquals("secret_device_123", apiCookies[0].value)

        // Request to synergia.librus.pl
        val synergiaCookies = storage.get(Url("https://synergia.librus.pl/gateway/api/2.0/Me"))
        assertEquals(1, synergiaCookies.size)
        assertEquals("oauth_token", synergiaCookies[0].name)
    }
}
