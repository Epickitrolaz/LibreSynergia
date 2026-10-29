package pl.librus.mobile

import pl.librus.mobile.auth.GatewayAuthClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GatewayAuthTest {

    @Test
    fun testBanerHeaderGeneration() {
        val baner = GatewayAuthClient.createBanerHeader()
        assertTrue(baner.isNotEmpty())
        assertTrue(baner.contains("_"))
        val parts = baner.split("_")
        assertEquals(2, parts.size)
        assertTrue(parts[0].isNotEmpty())
        assertTrue(parts[1].isNotEmpty())
    }

    @Test
    fun testBanerEncoding() {
        val encoded = GatewayAuthClient.encodeBanerSegment("12345")
        // '1' (49) + 20 = 69 ('E')
        assertEquals("EFGHI", encoded)
    }
}
