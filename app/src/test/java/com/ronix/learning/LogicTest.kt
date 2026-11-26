package com.ronix.learning

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class LogicTest {
    @Test fun youtubeFormats() {
        assertEquals("dQw4w9WgXcQ", youtubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", youtubeId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", youtubeId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertNull(youtubeId("https://example.com/watch?v=dQw4w9WgXcQ"))
        assertNull(youtubeId("not a link"))
    }
    @Test fun progress() {
        assertEquals(0, progressPercent(0, 0))
        assertEquals(50, progressPercent(1, 2))
        assertEquals(100, progressPercent(5, 3))
    }
    @Test fun streakCounts() {
        val t = LocalDate.of(2026, 11, 26)
        assertEquals(3, streak(listOf("2026-11-26", "2026-11-25", "2026-11-24"), t))
        assertEquals(2, streak(listOf("2026-11-25", "2026-11-24"), t))
        assertEquals(0, streak(listOf("2026-11-20"), t))
    }
}
