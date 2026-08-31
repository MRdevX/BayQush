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
}
