package com.example.data.repository

import com.example.data.api.AnimeApiService
import com.example.data.model.AnimeDetailData
import com.example.data.model.AnimeEpisodeItem
import com.example.data.model.AnimeItem
import com.example.data.model.StreamDownloadItem
import com.example.data.model.StreamResponse
import com.example.data.model.formatImageUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder

class AnimeRepository(
    private val apiService: AnimeApiService = AnimeApiService.create()
) {

    companion object {
        val STUDIO_KEYWORDS = mapOf(
            "MAPPA" to listOf("Jujutsu", "Chainsaw", "Shingeki no Kyojin: The Final", "Vinland", "Jigokuraku", "Hell's Paradise", "Dororo", "Zombieland", "Kakegurui", "Yuri on Ice", "Taisou Samurai", "Inuyashiki", "Dorohedoro"),
            "Ufotable" to listOf("Kimetsu", "Demon Slayer", "Fate", "Kara no Kyoukai", "Tales of Zestiria", "God Eater"),
            "Toei Animation" to listOf("One Piece", "Dragon Ball", "World Trigger", "Sailor Moon", "Slam Dunk", "Digimon", "Toriko", "Precure"),
            "Pierrot" to listOf("Naruto", "Black Clover", "Bleach", "Boruto", "Tokyo Ghoul", "Yu Yu Hakusho", "Great Teacher Onizuka", "Osomatsu", "Akatsuki no Yona"),
            "A-1 Pictures" to listOf("Solo Leveling", "Ore dake Level Up", "Fairy Tail", "Sword Art Online", "Kaguya-sama", "86", "Shigatsu wa Kimi no Uso", "Your Lie in April", "Mashle", "Erased", "Boku dake ga Inai Machi", "Anohana", "Blue Exorcist", "Nanatsu no Taizai"),
            "CloverWorks" to listOf("Kaoru Hana", "Wind Breaker", "Horimiya", "Spy x Family", "Bocchi the Rock", "Sono Bisque", "My Dress-Up Darling", "Promised Neverland", "Yakusoku no Neverland", "Bunny Girl Senpai", "Akebi", "Wonder Egg Priority"),
            "Bones" to listOf("Boku no Hero", "Hero Academia", "Fullmetal Alchemist", "Mob Psycho", "Bungou Stray Dogs", "Noragami", "Soul Eater", "Space Dandy", "Blood Blockade Battlefront", "Kekkai Sensen", "Ouran"),
            "Madhouse" to listOf("Hunter x Hunter", "Death Note", "One Punch Man", "Frieren", "Sousou no Frieren", "Overlord", "No Game No Life", "Parasyte", "Black Lagoon", "Hajime no Ippo", "Monster", "Hellsing Ultimate"),
            "Production I.G" to listOf("Haikyuu", "Kuroko no Basket", "Psycho-Pass", "Ghost in the Shell", "Ao Haru Ride", "Kimi ni Todoke", "Guilty Crown", "Kaiju No. 8"),
            "Wit Studio" to listOf("Shingeki no Kyojin", "Attack on Titan", "Vinland Saga", "Ranking of Kings", "Ousama Ranking", "Great Pretender", "Spy x Family", "Vivy", "Seraph of the End", "Owari no Seraph", "Kabaneri"),
            "Kyoto Animation" to listOf("Violet Evergarden", "Koe no Katachi", "A Silent Voice", "Clannad", "Hyouka", "K-On", "Free!", "Kobayashi-san", "Nichijou", "Beyond the Boundary", "Kyoukai no Kanata", "Hibike! Euphonium", "Tamako Market", "Chuunibyou"),
            "J.C.Staff" to listOf("DanMachi", "Dungeon ni Deai", "Toradora", "Shokugeki", "Food Wars", "Toaru Majutsu no Index", "Toaru Kagaku no Railgun", "Saiki K", "Golden Time", "Zero no Tsukaima", "Bakuman", "Slayers"),
            "Trigger" to listOf("Kill la Kill", "Cyberpunk: Edgerunners", "Dungeon Meshi", "Delicious in Dungeon", "Darling in the Franxx", "Gridman", "Little Witch Academia", "Promare", "BNA", "Kiznaiver"),
            "CoMix Wave Films" to listOf("Kimi no Na wa", "Your Name", "Suzume", "Weathering With You", "Tenki no Ko", "5 Centimeters per Second", "Garden of Words")
        )
    }

    private val catalogPool = mutableListOf<AnimeItem>()
    private val poolLock = Any()

    suspend fun getCatalogPool(): List<AnimeItem> = withContext(Dispatchers.IO) {
        synchronized(poolLock) {
            if (catalogPool.isNotEmpty()) {
                return@withContext catalogPool.toList()
            }
        }

        try {
            val p0 = apiService.exploreMovie(page = 0, sort = "views").data?.movie ?: emptyList()
            val p1 = apiService.exploreMovie(page = 1, sort = "views").data?.movie ?: emptyList()
            val pLatest = apiService.exploreMovie(page = 0, sort = "latest").data?.movie ?: emptyList()
            val pAlpha = apiService.exploreMovie(page = 0, sort = "alphabet").data?.movie ?: emptyList()

            val combined = (p0 + p1 + pLatest + pAlpha).distinctBy { it.id }
            synchronized(poolLock) {
                catalogPool.clear()
                catalogPool.addAll(combined)
                catalogPool.toList()
            }
        } catch (e: Exception) {
            synchronized(poolLock) {
                if (catalogPool.isNotEmpty()) catalogPool.toList() else emptyList()
            }
        }
    }

    suspend fun searchAnime(
        keyword: String = "",
        genre: String? = null,
        studio: String? = null,
        sort: String = "views",
        page: Int = 0
    ): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        try {
            val kw = keyword.trim()
            val cleanGenre = genre?.takeIf { it.isNotBlank() && !it.equals("semua", ignoreCase = true) }
            val cleanStudio = studio?.takeIf { it.isNotBlank() && !it.equals("semua", ignoreCase = true) }

            // 1. Gather source items
            val baseList: List<AnimeItem>
            if (kw.isNotBlank()) {
                // Fetch server search results
                val serverResp = apiService.exploreMovie(page = page, sort = sort, keyword = kw)
                val serverList = serverResp.data?.movie ?: emptyList()

                // Also check local pool for fuzzy matching
                val pool = getCatalogPool()
                val localMatches = pool.filter { item ->
                    item.title.contains(kw, ignoreCase = true) ||
                    item.synopsis?.contains(kw, ignoreCase = true) == true
                }
                baseList = (serverList + localMatches).distinctBy { it.id }
            } else {
                val pool = getCatalogPool()
                baseList = if (pool.isNotEmpty()) {
                    pool
                } else {
                    val fallback = apiService.exploreMovie(page = page, sort = sort)
                    fallback.data?.movie ?: emptyList()
                }
            }

            // 2. Filter by Genre (client-side because backend ignores genre_in)
            val genreFiltered = if (cleanGenre != null) {
                baseList.filter { item ->
                    if (cleanGenre.equals("Isekai", ignoreCase = true)) {
                        item.genre?.contains("Isekai", ignoreCase = true) == true ||
                        item.title.contains("Isekai", ignoreCase = true) ||
                        item.title.contains("Tensei", ignoreCase = true) ||
                        item.synopsis?.contains("isekai", ignoreCase = true) == true ||
                        item.synopsis?.contains("reinkarnasi", ignoreCase = true) == true
                    } else {
                        item.genre?.contains(cleanGenre, ignoreCase = true) == true
                    }
                }
            } else {
                baseList
            }

            // 3. Filter by Studio (supports direct attribute & iconic franchise mappings)
            val studioFiltered = if (cleanStudio != null) {
                val keywords = STUDIO_KEYWORDS[cleanStudio] ?: listOf(cleanStudio)
                genreFiltered.filter { item ->
                    item.studio?.contains(cleanStudio, ignoreCase = true) == true ||
                    keywords.any { kwItem ->
                        item.title.contains(kwItem, ignoreCase = true) ||
                        item.synopsis?.contains(kwItem, ignoreCase = true) == true
                    }
                }
            } else {
                genreFiltered
            }

            // 4. Apply Sort By
            val sortedList = when (sort.lowercase()) {
                "latest", "terbaru" -> {
                    studioFiltered.sortedWith(
                        compareByDescending<AnimeItem> { it.year?.toIntOrNull() ?: 0 }
                            .thenByDescending { it.views?.toLongOrNull() ?: 0L }
                    )
                }
                "rating", "score", "rating tertinggi" -> {
                    studioFiltered.sortedWith(
                        compareByDescending<AnimeItem> { it.numericScore }
                            .thenByDescending { it.views?.toLongOrNull() ?: 0L }
                    )
                }
                "alphabet", "abjad", "title" -> {
                    studioFiltered.sortedBy { it.title.lowercase() }
                }
                else -> { // "views", "terpopuler"
                    studioFiltered.sortedByDescending { it.views?.toLongOrNull() ?: 0L }
                }
            }

            Result.success(sortedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSchedule(day: String): Result<List<AnimeItem>> = withContext(Dispatchers.IO) {
        val cleanDay = day.trim().lowercase()
        try {
            val response = apiService.getScheduleData(day = cleanDay)
            val list = response.data?.movie ?: emptyList()
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAnimeDetail(animeId: String): Result<AnimeDetailData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMovieDetail(animeId = animeId)
            val data = response.data
            if (data != null) {
                Result.success(data)
            } else {
                Result.failure(Exception("Detail anime tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAnimeEpisodes(animeId: String): Result<List<AnimeEpisodeItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMovieEpisodes(animeId = animeId)
            val episodes = response.data?.episode ?: emptyList()
            Result.success(episodes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEpisodeStream(episodeId: String): Result<StreamResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getEpisodeStreamNew(episodeId = episodeId)
            val streamData = response.data
            val allServers = streamData?.server ?: emptyList()

            // Filter direct servers as in the scraper, fallback to all servers if no direct servers
            val directServers = allServers.filter { it.type.equals("direct", ignoreCase = true) }
            val chosenServers = if (directServers.isNotEmpty()) directServers else allServers

            // Derive title
            var extractedTitle: String? = streamData?.episode?.title
            if (extractedTitle.isNullOrBlank() && chosenServers.isNotEmpty()) {
                try {
                    val rawLink = URLDecoder.decode(chosenServers[0].link, "UTF-8")
                    val parts = rawLink.split("/")
                    if (parts.size > 3) {
                        extractedTitle = parts[3].replace("-", " ")
                    }
                } catch (_: Exception) {}
            }

            val thumbnail = formatImageUrl(streamData?.episode?.image)

            val streamResponse = StreamResponse(
                title = extractedTitle ?: "Episode Stream",
                thumbnail = thumbnail,
                download = chosenServers
            )
            Result.success(streamResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
