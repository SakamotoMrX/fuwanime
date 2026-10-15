package com.fuwanime.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuwanime.data.Anime
import com.fuwanime.data.AnimeRepository
import com.fuwanime.data.Episode
import com.fuwanime.data.LibraryRepository
import com.fuwanime.data.StreamSource
import com.fuwanime.data.local.AppSettings
import com.fuwanime.data.local.SettingsStore
import com.fuwanime.util.friendlyMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

data class PlayerUiState(
    val anime: Anime? = null,
    val episode: Episode,
    val sources: List<StreamSource> = emptyList(),
    val source: StreamSource? = null,
    /** Position to seek to once the current source is prepared. */
    val startPositionMs: Long = 0,
    val loading: Boolean = true,
    val error: String? = null,
    val next: Episode? = null,
    val previous: Episode? = null,
    /** Episodes of the current page, for the portrait episode strip. */
    val nearby: List<Episode> = emptyList(),
)

class PlayerViewModel(
    private val repo: AnimeRepository,
    private val library: LibraryRepository,
    settingsStore: SettingsStore,
    private val appScope: CoroutineScope,
    saved: SavedStateHandle,
) : ViewModel() {

    private val animeId: String = checkNotNull(saved["animeId"])

    private val _state = MutableStateFlow(
        PlayerUiState(
            anime = repo.cachedAnime(animeId),
            episode = Episode(
                id = checkNotNull(saved["episodeId"]),
                number = saved.get<String>("number").orEmpty(),
                title = "Episode ${saved.get<String>("number").orEmpty()}",
            ),
        ),
    )
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    val settings: StateFlow<AppSettings> =
        settingsStore.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private var loadJob: Job? = null

    init {
        if (_state.value.anime == null) {
            viewModelScope.launch {
                repo.detail(animeId).onSuccess { a -> _state.update { it.copy(anime = a) } }
            }
        }
        play(_state.value.episode)
    }

    fun play(episode: Episode) {
        loadJob?.cancel()
        _state.update {
            it.copy(episode = episode, sources = emptyList(), source = null, loading = true, error = null, next = null, previous = null)
        }
        loadJob = viewModelScope.launch {
            val resumeAt = library.savedPosition(episode.id)
            val stream = repo.stream(episode.id).getOrElse { e ->
                _state.update { it.copy(loading = false, error = e.friendlyMessage()) }
                return@launch
            }
            if (stream.sources.isEmpty()) {
                _state.update { it.copy(loading = false, error = "Belum ada sumber video untuk episode ini.") }
                return@launch
            }
            val merged = stream.episode?.let { info ->
                episode.copy(
                    title = info.title.ifBlank { episode.title },
                    thumbnail = info.thumbnail.ifBlank { episode.thumbnail },
                    number = episode.number.ifBlank { info.number },
                )
            } ?: episode
            val preferred = settings.first().preferredQuality
            _state.update {
                it.copy(
                    episode = merged,
                    sources = stream.sources,
                    source = pickSource(stream.sources, preferred),
                    startPositionMs = resumeAt,
                    loading = false,
                )
            }
            loadNeighbours(merged)
        }
    }

    private fun pickSource(sources: List<StreamSource>, preferredHeight: Int): StreamSource =
        sources.minByOrNull { s -> if (s.height == 0) Int.MAX_VALUE else abs(s.height - preferredHeight) } ?: sources.first()

    private suspend fun loadNeighbours(episode: Episode) {
        val next = repo.adjacentEpisode(animeId, episode, newer = true)
        val prev = repo.adjacentEpisode(animeId, episode, newer = false)
        val page = repo.pageOf(animeId, episode.id)
        val nearby = page?.let { repo.episodePage(animeId, it).getOrNull() }?.sortedBy { it.sortKey }.orEmpty()
        if (_state.value.episode.id == episode.id) {
            _state.update { it.copy(next = next, previous = prev, nearby = nearby) }
        }
    }

    fun selectSource(source: StreamSource, currentPositionMs: Long) {
        _state.update { it.copy(source = source, startPositionMs = currentPositionMs) }
    }

    fun playNext() {
        _state.value.next?.let(::play)
    }

    fun playPrevious() {
        _state.value.previous?.let(::play)
    }

    fun retry() = play(_state.value.episode)

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val s = _state.value
        val anime = s.anime ?: return
        // Ignore the first few seconds so merely opening an episode doesn't clutter history.
        if (durationMs <= 0 || positionMs < 5_000) return
        appScope.launch { library.saveProgress(anime, s.episode, positionMs, durationMs) }
    }
}
