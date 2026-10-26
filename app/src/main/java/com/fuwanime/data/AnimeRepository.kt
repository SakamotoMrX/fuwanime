package com.fuwanime.data

import com.fuwanime.data.remote.AnimeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

enum class SortOrder(val apiValue: String, val label: String) {
    POPULAR("views", "Terpopuler"),
    TITLE("alphabet", "A–Z"),
}

/**
 * Remote data with a small in-memory cache so screens can show what they already know
 * (e.g. a poster tapped on Home) while the full payload loads.
 */
class AnimeRepository(private val api: AnimeApiService) {

    private val animeCache = ConcurrentHashMap<String, Anime>()

    /** animeId -> (page -> episodes newest-first). Pages are fixed windows of 30 from the API. */
    private val episodePages = ConcurrentHashMap<String, MutableMap<Int, List<Episode>>>()
    private val episodeLock = Mutex()

    fun cachedAnime(id: String): Anime? = animeCache[id]

    private fun remember(list: List<Anime>): List<Anime> {
        list.forEach { anime -> animeCache.merge(anime.id, anime, ::mergeAnime) }
        return list
    }

    /** Detail responses carry fields (studio) list responses lack, and vice versa. */
    private fun mergeAnime(old: Anime, new: Anime) = new.copy(
        studio = new.studio.ifBlank { old.studio },
        nextAirTime = new.nextAirTime.ifBlank { old.nextAirTime },
        cover = new.cover.ifBlank { old.cover },
        poster = new.poster.ifBlank { old.poster },
    )

    suspend fun explore(
        page: Int,
        sort: SortOrder = SortOrder.POPULAR,
        keyword: String = "",
        type: String? = null,
    ): Result<List<Anime>> =
        io {
            val items = api.explore(page = page, sort = sort.apiValue, keyword = keyword.trim()).data?.movie.orEmpty()
            remember(items.map { it.toAnime() }.distinctBy { it.id })
        }

    suspend fun schedule(day: String): Result<List<Anime>> = io {
        remember(api.schedule(day.lowercase()).data?.movie.orEmpty().map { it.toAnime() })
    }

    /** Detail plus the franchise list ("S1", "Movie", …) that only the detail response carries. */
    suspend fun detailWithSeasons(id: String): Result<AnimeDetail> = io {
        val data = api.detail(id).data
        val anime = data?.movie?.toAnime() ?: error("Anime tidak ditemukan")
        val seasons = data.season.orEmpty()
            .filter { it.id.isNotBlank() }
            .map { SeasonEntry(it.toAnime(), it.season?.trim().orEmpty()) }
        AnimeDetail(animeCache.merge(id, anime, ::mergeAnime)!!, seasons)
    }

    suspend fun detail(id: String): Result<Anime> = detailWithSeasons(id).map { it.anime }

    suspend fun episodePage(animeId: String, page: Int, forceRefresh: Boolean = false): Result<List<Episode>> = io {
        if (!forceRefresh) {
            episodePages[animeId]?.get(page)?.let { return@io it }
        }
        val episodes = api.episodes(animeId, page = page).data?.episode.orEmpty().map { it.toEpisode() }
        episodeLock.withLock {
            episodePages.getOrPut(animeId) { ConcurrentHashMap() }[page] = episodes
        }
        episodes
    }

    /** Server-side substring match on the episode number, e.g. "10" -> 10, 100–109, 110, 210… */
    suspend fun searchEpisodes(animeId: String, query: String): Result<List<Episode>> = io {
        api.episodes(animeId, page = 0, search = query.trim()).data?.episode.orEmpty().map { it.toEpisode() }
    }

    fun pageOf(animeId: String, episodeId: String): Int? =
        episodePages[animeId]?.entries?.firstOrNull { (_, eps) -> eps.any { it.id == episodeId } }?.key

    /**
     * Finds the chronologically adjacent episode, loading the neighbouring page when the
     * episode sits on a page boundary. [newer] = true means the next episode to watch.
     */
    suspend fun adjacentEpisode(animeId: String, episode: Episode, newer: Boolean): Episode? {
        val page = pageOf(animeId, episode.id) ?: locatePage(animeId, episode) ?: return null
        val list = episodePages[animeId]?.get(page) ?: return null
        val i = list.indexOfFirst { it.id == episode.id }
        if (i < 0) return null
        // Pages are newest-first, so "newer" means a lower index.
        return if (newer) {
            if (i > 0) list[i - 1]
            else if (page > 0) episodePage(animeId, page - 1).getOrNull()?.lastOrNull()
            else null
        } else {
            if (i < list.lastIndex) list[i + 1]
            else episodePage(animeId, page + 1).getOrNull()?.firstOrNull()
        }
    }

    /** For episodes opened from history: estimate the page from the episode number, then verify. */
    private suspend fun locatePage(animeId: String, episode: Episode): Int? {
        val latest = episodePage(animeId, 0).getOrNull()?.firstOrNull()?.sortKey ?: return null
        pageOf(animeId, episode.id)?.let { return it }
        val target = episode.sortKey
        if (target == Double.MAX_VALUE) return null
        val estimate = ((latest - target) / AnimeApiService.EPISODE_PAGE_SIZE).toInt().coerceAtLeast(0)
        for (candidate in listOf(estimate, estimate - 1, estimate + 1).filter { it >= 0 }) {
            val eps = episodePage(animeId, candidate).getOrNull() ?: continue
            if (eps.any { it.id == episode.id }) return candidate
        }
        return null
    }

    suspend fun stream(episodeId: String): Result<EpisodeStream> = io {
        val data = api.stream(episodeId).data ?: error("Stream tidak tersedia")
        val servers = data.server.orEmpty().filter { it.link.isNotBlank() }
        // Prefer direct files; other types are embed pages the native player can't open.
        val direct = servers.filter { it.type.equals("direct", ignoreCase = true) }
        val sources = (direct.ifEmpty { servers }).map { it.toSource() }
            .distinctBy { it.url }
            .sortedByDescending { it.height }
        EpisodeStream(episode = data.episode?.toEpisode(), sources = sources)
    }

    private suspend fun <T> io(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
