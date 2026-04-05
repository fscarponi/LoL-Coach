package com.lolcoach.bridge.lockfile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LockfileReaderTest {

    @Test
    fun `parse valid lockfile content`() {
        val content = "LeagueClient:12345:8394:abcdef123456:https"
        val data = LockfileReader.parse(content)

        assertEquals("LeagueClient", data.processName)
        assertEquals(12345, data.pid)
        assertEquals(8394, data.port)
        assertEquals("abcdef123456", data.token)
        assertEquals("https", data.protocol)
    }

    @Test
    fun `parse lockfile content with trailing newline`() {
        val content = "LeagueClient:9999:51234:mytoken:https\n"
        val data = LockfileReader.parse(content)

        assertEquals("LeagueClient", data.processName)
        assertEquals(9999, data.pid)
        assertEquals(51234, data.port)
        assertEquals("mytoken", data.token)
        assertEquals("https", data.protocol)
    }

    @Test
    fun `parse invalid lockfile throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            LockfileReader.parse("invalid:content")
        }
    }

    @Test
    fun `parse empty lockfile throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            LockfileReader.parse("")
        }
    }

    @Test
    fun `readFromFile returns null for nonexistent file`() {
        val result = LockfileReader.readFromFile("/nonexistent/path/lockfile")
        assertNull(result)
    }
}
