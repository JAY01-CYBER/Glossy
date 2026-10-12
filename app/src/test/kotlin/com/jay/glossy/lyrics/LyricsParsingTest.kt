package com.jay.glossy.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Timed lyrics only work if the gate that calls text "synced" and the parser
 * that reads it agree. They used to be two different regexes — the gate's was
 * looser — so a file the app had just called timed parsed to zero timed lines
 * and the player announced "Lyrics aren't time-synced" for it. These tests pin
 * every shape that gap used to swallow.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class LyricsParsingTest {
    private fun parsed(lyrics: String) = LyricsUtils.parseLyrics(lyrics)

    @Test
    fun `single digit minutes are timed`() {
        val lines = parsed("[0:12.34]First\n[0:15.00]Second")

        assertEquals(2, lines.size)
        // `0:12.34` is twelve point three four *seconds*, not twelve minutes.
        assertEquals(12_340L, lines[0].time)
        assertEquals(15_000L, lines[1].time)
    }

    @Test
    fun `a stamp without a fraction lands on the second`() {
        val lines = parsed("[00:12]First\n[00:15]Second")

        assertEquals(2, lines.size)
        assertEquals(12_000L, lines[0].time)
    }

    @Test
    fun `fraction width decides the scale`() {
        // Tenths, hundredths, milliseconds: reading a narrow fraction as plain
        // milliseconds would start the line hundreds of milliseconds early.
        assertEquals(12_300L, parsed("[00:12.3]Line").first().time)
        assertEquals(12_340L, parsed("[00:12.34]Line").first().time)
        assertEquals(12_345L, parsed("[00:12.345]Line").first().time)
    }

    @Test
    fun `several stamps on one line produce one entry each`() {
        val lines = parsed("[00:12.34][00:15.00]Repeated")

        assertEquals(2, lines.size)
        assertEquals(listOf("Repeated", "Repeated"), lines.map { it.text })
    }

    @Test
    fun `a payload collapsed onto one line is split at every stamp`() {
        // A provider that returns one run per lyric line without a line break
        // between them delivers the whole song as a single physical line.
        val lines = parsed("[00:12.34]First[00:15.00]Second[00:18.00]Third")

        assertEquals(3, lines.size)
        assertEquals(listOf("First", "Second", "Third"), lines.map { it.text })
    }

    @Test
    fun `well formed lyrics keep their own line breaks`() {
        val lines = parsed("[00:12.34]First\n\n[00:15.00]Second")

        assertEquals(listOf("First", "Second"), lines.map { it.text })
    }

    @Test
    fun `word level timings count as timed`() {
        val lines = parsed("<00:01.50>Hello <00:02.00>world")

        assertEquals(1, lines.size)
        assertEquals(1_500L, lines.first().time)
        assertEquals("Hello world", lines.first().text)
    }

    @Test
    fun `plain lyrics parse to nothing`() {
        val plain = "Some words\nwithout any timing at all"

        assertTrue(parsed(plain).isEmpty())
        assertFalse(lyricsTextLooksSynced(plain))
    }

    @Test
    fun `the sync gate agrees with the parser for every timed shape`() {
        val timed =
            listOf(
                "[0:12.34]Line",
                "[00:12]Line",
                "[00:12.3]Line",
                "[00:12.345]Line",
                "[00:12.34]First[00:15.00]Second",
                "<00:01.50>Hello <00:02.00>world",
            )

        timed.forEach { text ->
            assertTrue("\"$text\" should look synced", lyricsTextLooksSynced(text))
            assertTrue("\"$text\" should parse", parsed(text).isNotEmpty())
        }
    }

    @Test
    fun `blank and missing lyrics are not synced`() {
        assertFalse(lyricsTextLooksSynced(null))
        assertFalse(lyricsTextLooksSynced(""))
        assertFalse(lyricsTextLooksSynced("   "))
    }
}
