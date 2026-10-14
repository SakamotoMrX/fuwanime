package com.example

import com.example.data.api.AnimeApiService
import com.example.data.model.formatImageUrl
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun formatImageUrl_formatsRelativeAndCrunchyrollUrls() {
        val relative = formatImageUrl("/images/poster.jpg")
        assertEquals("https://xyz-api.animein.net/images/poster.jpg", relative)

        val protocolRelative = formatImageUrl("//images/poster.jpg")
        assertEquals("https://images/poster.jpg", protocolRelative)

        val crunchyroll = formatImageUrl("https://img1.ak.crunchyroll.com/i/spire4/poster")
        assertEquals("https://img1.ak.crunchyroll.com/i/spire4/poster_full.jpg", crunchyroll)
    }

    @Test
    fun apiService_baseUrlMatchesScraper() {
        assertEquals("https://xyz-api.animein.net/", AnimeApiService.BASE_URL)
        assertEquals("396454", AnimeApiService.DEFAULT_USER_ID)
        assertEquals("Di63beJZx6l7uPYKXZqzEdANGhsi2e77AFbUB8rgqSzXiRh2pi", AnimeApiService.DEFAULT_CLIENT_KEY)
    }

    @Test
    fun apiService_liveSearchAndScheduleCall() = runTest {
        val repo = AnimeRepository()
        val searchResult = repo.searchAnime("one piece")
        assertTrue("Search should succeed", searchResult.isSuccess)
        val movies = searchResult.getOrNull()
        assertNotNull(movies)
        assertTrue("Should return movies for one piece", movies!!.isNotEmpty())
        assertTrue("First movie should have id", movies[0].id.isNotBlank())

        val scheduleResult = repo.getSchedule("senin")
        assertTrue("Schedule should succeed", scheduleResult.isSuccess)
        val scheduleMovies = scheduleResult.getOrNull()
        assertNotNull(scheduleMovies)
        assertTrue("Schedule for senin should have movies", scheduleMovies!!.isNotEmpty())
    }

    @Test
    fun playbackPosition_preservation() {
        val vm = com.example.ui.viewmodel.AnimeViewModel()
        assertEquals(0L, vm.playerState.value.savedPositionMs)

        vm.updatePlaybackPosition(45000L)
        assertEquals(45000L, vm.playerState.value.savedPositionMs)

        // Invalid or 0 position should not overwrite
        vm.updatePlaybackPosition(0L)
        assertEquals(45000L, vm.playerState.value.savedPositionMs)

        vm.resetPlaybackPosition()
        assertEquals(0L, vm.playerState.value.savedPositionMs)
    }

    @Test
    fun catalog_genresAndStudiosFilter() {
        val vm = com.example.ui.viewmodel.AnimeViewModel()
        assertTrue(vm.genres.contains("Action"))
        assertTrue(vm.genres.contains("Romance"))
        assertTrue(vm.studios.contains("MAPPA"))
        assertTrue(vm.studios.contains("Toei Animation"))
        assertTrue(vm.sortOptions.any { it.first == "alphabet" })

        vm.selectCatalogGenre("Romance")
        assertEquals("Romance", vm.homeState.value.catalogFilter.selectedGenre)

        // Toggling same genre resets to Semua
        vm.selectCatalogGenre("Romance")
        assertEquals("Semua", vm.homeState.value.catalogFilter.selectedGenre)

        vm.selectCatalogStudio("MAPPA")
        assertEquals("MAPPA", vm.homeState.value.catalogFilter.selectedStudio)

        vm.selectCatalogStudio("MAPPA")
        assertEquals("Semua", vm.homeState.value.catalogFilter.selectedStudio)

        vm.selectCatalogSort("latest")
        assertEquals("latest", vm.homeState.value.catalogFilter.selectedSort)
    }

    @Test
    fun repository_genreFiltering_worksCorrectly() = runTest {
        val repo = AnimeRepository()
        val romanceResult = repo.searchAnime(genre = "Romance")
        assertTrue("Romance filter should succeed", romanceResult.isSuccess)
        val romanceList = romanceResult.getOrNull() ?: emptyList()
        assertTrue("Should have romance anime", romanceList.isNotEmpty())
        assertTrue("All items must have Romance in genre", romanceList.all { it.genre?.contains("Romance", ignoreCase = true) == true })

        val actionResult = repo.searchAnime(genre = "Action")
        assertTrue("Action filter should succeed", actionResult.isSuccess)
        val actionList = actionResult.getOrNull() ?: emptyList()
        assertTrue("Should have action anime", actionList.isNotEmpty())
        assertTrue("All items must have Action in genre", actionList.all { it.genre?.contains("Action", ignoreCase = true) == true })
    }

    @Test
    fun repository_studioFiltering_worksCorrectly() = runTest {
        val repo = AnimeRepository()
        val toeiResult = repo.searchAnime(studio = "Toei Animation")
        assertTrue("Toei Animation should succeed", toeiResult.isSuccess)
        val toeiList = toeiResult.getOrNull() ?: emptyList()
        assertTrue("Toei should have anime like One Piece or Dragon Ball", toeiList.isNotEmpty())
        assertTrue("Toei should contain One Piece", toeiList.any { it.title.contains("One Piece", ignoreCase = true) })

        val mappaResult = repo.searchAnime(studio = "MAPPA")
        assertTrue("MAPPA should succeed", mappaResult.isSuccess)
        val mappaList = mappaResult.getOrNull() ?: emptyList()
        assertTrue("MAPPA should have anime like Jujutsu Kaisen or Shingeki", mappaList.isNotEmpty())
        assertTrue("MAPPA should contain Jujutsu", mappaList.any { it.title.contains("Jujutsu", ignoreCase = true) })
    }

    @Test
    fun repository_sortingOptions_workCorrectly() = runTest {
        val repo = AnimeRepository()

        // 1. Terpopuler (views)
        val viewsResult = repo.searchAnime(sort = "views")
        assertTrue(viewsResult.isSuccess)
        val viewsList = viewsResult.getOrNull() ?: emptyList()
        assertTrue(viewsList.size > 2)
        val v0 = viewsList[0].views?.toLongOrNull() ?: 0L
        val v1 = viewsList[1].views?.toLongOrNull() ?: 0L
        assertTrue("Views should be descending: $v0 >= $v1", v0 >= v1)

        // 2. Terbaru (latest)
        val latestResult = repo.searchAnime(sort = "latest")
        assertTrue(latestResult.isSuccess)
        val latestList = latestResult.getOrNull() ?: emptyList()
        assertTrue(latestList.size > 2)
        val y0 = latestList[0].year?.toIntOrNull() ?: 0
        val yLast = latestList.last().year?.toIntOrNull() ?: 0
        assertTrue("Year should be descending: $y0 >= $yLast", y0 >= yLast)

        // 3. Rating Tertinggi (rating)
        val ratingResult = repo.searchAnime(sort = "rating")
        assertTrue(ratingResult.isSuccess)
        val ratingList = ratingResult.getOrNull() ?: emptyList()
        assertTrue(ratingList.size > 2)
        val r0 = ratingList[0].numericScore
        val r1 = ratingList[1].numericScore
        assertTrue("Score should be descending: $r0 >= $r1", r0 >= r1)

        // 4. A - Z (alphabet)
        val alphaResult = repo.searchAnime(sort = "alphabet")
        assertTrue(alphaResult.isSuccess)
        val alphaList = alphaResult.getOrNull() ?: emptyList()
        assertTrue(alphaList.size > 2)
        val t0 = alphaList[0].title.lowercase()
        val t1 = alphaList[1].title.lowercase()
        assertTrue("Titles should be alphabetical: $t0 <= $t1", t0 <= t1)
    }
}
