package me.mrashidi.bayqush

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun formatSms_joinsFromAndBody() {
        assertEquals("From: +1\n\nhello", formatSms("+1", "hello"))
    }

    @Test
    fun formatSms_truncatesAt4096() {
        val body = "x".repeat(5000)
        val out = formatSms("a", body)
        assertEquals(4096, out.length)
        assertTrue(out.startsWith("From: a\n\n"))
    }

    @Test
    fun sameSender_matchesCountryCode() {
        assertTrue(sameSender("+15551234567", "5551234567"))
    }

    @Test
    fun shouldForward_specificSendersOnly() {
        val allowed = setOf("+15551234567")
        assertTrue(shouldForward("5551234567", forwardAll = false, allowed))
        assertTrue(!shouldForward("999", forwardAll = false, allowed))
        assertTrue(shouldForward("999", forwardAll = true, allowed))
    }
}
