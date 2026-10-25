package com.fuwanime

import com.fuwanime.data.Anime
import com.fuwanime.data.StreamSource
import com.fuwanime.data.closestTo
import com.fuwanime.data.imageUrl
import com.fuwanime.data.remote.EpisodeDto
import com.fuwanime.data.toEpisode
import com.fuwanime.util.airTime
import com.fuwanime.util.compactNumber
import com.fuwanime.util.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun imageUrl_normalisesRelativePaths() {
        assertEquals("https://xyz-api.animein.net/a/b.jpg", imageUrl("/a/b.jpg"))
        assertEquals("https://xyz-api.animein.net/a/b.jpg", imageUrl("a/b.jpg"))
        assertEquals("https://cdn.example/x.jpg", imageUrl("//cdn.example/x.jpg"))
        assertEquals("", imageUrl("  "))
    }

    @Test
    fun compactNumber_usesIndonesianUnits() {
        assertEquals("15,9 jt", compactNumber(15_894_862))
        assertEquals("38,8 rb", compactNumber(38_798))
        assertEquals("2 rb", compactNumber(2_000))
        assertEquals("999", compactNumber(999))
    }

    @Test
    fun formatDuration_switchesToHours() {
        assertEquals("01:05", formatDuration(65_000))
        assertEquals("1:00:00", formatDuration(3_600_000))
    }

    @Test
    fun episodeIndex_parsesStringsAndDoubles() {
        assertEquals("1180", EpisodeDto(id = "1", index = "1180").toEpisode().number)
        assertEquals("12", EpisodeDto(id = "1", index = 12.0).toEpisode().number)
        assertEquals("12.5", EpisodeDto(id = "1", index = 12.5).toEpisode().number)
    }

    @Test
    fun streamSource_parsesHeight() {
        assertEquals(720, StreamSource("u", "720p", "s").height)
        assertEquals(0, StreamSource("u", "Auto", "s").height)
    }

    @Test
    fun closestTo_picksNearestHeight_andRanksUnknownLast() {
        val sources = listOf(
            StreamSource("a", "360p", "s1"),
            StreamSource("b", "720p", "s2"),
            StreamSource("c", "1080p", "s3"),
        )
        assertEquals("a", sources.closestTo(480)?.url)
        assertEquals("c", sources.closestTo(1080)?.url)
        assertEquals("a", sources.closestTo(200)?.url)
        // Unlabeled sources must lose against any labeled one.
        val unknown = listOf(StreamSource("x", "Auto", "s"), StreamSource("y", "480p", "s"))
        assertEquals("y", unknown.closestTo(720)?.url)
        assertEquals(null, emptyList<StreamSource>().closestTo(720))
    }

    @Test
    fun animeStatus_flags() {
        assert(Anime("a", "A", status = "ONGOING").isOngoing)
        assert(!Anime("a", "A", status = "COMPLETED").isUpcoming)
        assert(Anime("a", "A", status = "UPCOMING").isUpcoming)
        assert(Anime("a", "A", status = "not_yet_aired").isUpcoming)
        assert(!Anime("a", "A", status = "ONGOING").isUpcoming)
    }

    @Test
    fun airTime_extractsClock() {
        assertEquals("00:36", airTime("2026-10-22 00:36:10"))
        assertEquals("new !!", airTime("new !!"))
    }
}
