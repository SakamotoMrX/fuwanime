package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AnimeDetailData
import com.example.data.model.AnimeEpisodeItem
import com.example.data.model.AnimeItem
import com.example.data.model.StreamDownloadItem
import com.example.data.model.StreamResponse
import com.example.data.repository.AnimeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class HomeTab {
    SCHEDULE,
    CATALOG,
    SEARCH
}

data class CatalogFilter(
    val selectedGenre: String = "Semua",
    val selectedStudio: String = "Semua",
    val selectedSort: String = "views",
    val page: Int = 0
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val animeList: List<AnimeItem> = emptyList(),
    val catalogList: List<AnimeItem> = emptyList(),
    val errorMessage: String? = null,
    val activeTab: HomeTab = HomeTab.SCHEDULE,
    val selectedDay: String = "senin",
    val searchQuery: String = "",
    val searchGenre: String = "Semua",
    val searchStudio: String = "Semua",
    val catalogFilter: CatalogFilter = CatalogFilter()
)

data class DetailUiState(
    val isLoading: Boolean = false,
    val anime: AnimeItem? = null,
    val detail: AnimeDetailData? = null,
    val episodes: List<AnimeEpisodeItem> = emptyList(),
    val errorMessage: String? = null
)

data class PlayerUiState(
    val isLoading: Boolean = false,
    val animeTitle: String = "",
    val episodes: List<AnimeEpisodeItem> = emptyList(),
    val currentEpisode: AnimeEpisodeItem? = null,
    val streamData: StreamResponse? = null,
    val selectedQuality: StreamDownloadItem? = null,
    val activeStreamUrl: String = "",
    val errorMessage: String? = null,
    val savedPositionMs: Long = 0L
)

class AnimeViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    val daysOfWeek = listOf("senin", "selasa", "rabu", "kamis", "jumat", "sabtu", "minggu")

    val genres = listOf(
        "Semua", "Action", "Adventure", "Comedy", "Drama", "Fantasy",
        "Isekai", "Romance", "Sci-Fi", "Slice of Life", "Shounen",
        "Supernatural", "Mystery", "Sports", "Horror", "Psychological"
    )

    val studios = listOf(
        "Semua", "MAPPA", "Ufotable", "Toei Animation", "Kyoto Animation",
        "Madhouse", "A-1 Pictures", "CloverWorks", "Bones", "Wit Studio",
        "Pierrot", "CoMix Wave Films", "J.C.Staff", "Production I.G", "Trigger"
    )

    val sortOptions = listOf(
        "views" to "Terpopuler",
        "latest" to "Terbaru",
        "rating" to "Rating Tertinggi",
        "alphabet" to "A - Z (Abjad)"
    )

    private val _homeState = MutableStateFlow(
        HomeUiState(selectedDay = getTodayDayName())
    )
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _detailState = MutableStateFlow(DetailUiState())
    val detailState: StateFlow<DetailUiState> = _detailState.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    private var searchJob: Job? = null

    init {
        val today = getTodayDayName()
        _homeState.update { it.copy(selectedDay = today) }
        loadSchedule(today)
    }

    fun getTodayDayName(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "senin"
            Calendar.TUESDAY -> "selasa"
            Calendar.WEDNESDAY -> "rabu"
            Calendar.THURSDAY -> "kamis"
            Calendar.FRIDAY -> "jumat"
            Calendar.SATURDAY -> "sabtu"
            Calendar.SUNDAY -> "minggu"
            else -> "senin"
        }
    }

    fun switchTab(tab: HomeTab) {
        _homeState.update { it.copy(activeTab = tab, errorMessage = null) }
        when (tab) {
            HomeTab.SCHEDULE -> {
                loadSchedule(_homeState.value.selectedDay)
            }
            HomeTab.CATALOG -> {
                if (_homeState.value.catalogList.isEmpty()) {
                    loadCatalog()
                }
            }
            HomeTab.SEARCH -> {
                if (_homeState.value.searchQuery.isBlank() && _homeState.value.animeList.isEmpty()) {
                    executeSearch()
                }
            }
        }
    }

    fun selectDay(day: String) {
        val clean = day.lowercase()
        _homeState.update { it.copy(selectedDay = clean, activeTab = HomeTab.SCHEDULE) }
        loadSchedule(clean)
    }

    fun loadSchedule(day: String) {
        viewModelScope.launch {
            _homeState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getSchedule(day)
            result.onSuccess { list ->
                _homeState.update {
                    it.copy(
                        isLoading = false,
                        animeList = list,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _homeState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal memuat jadwal dari server (${error.localizedMessage ?: "Koneksi terputus"}). Ketuk Coba Lagi untuk memuat ulang."
                    )
                }
            }
        }
    }

    // --- KATALOG METHODS ---
    fun selectCatalogGenre(genre: String) {
        val newGenre = if (_homeState.value.catalogFilter.selectedGenre == genre) "Semua" else genre
        _homeState.update {
            it.copy(catalogFilter = it.catalogFilter.copy(selectedGenre = newGenre, page = 0))
        }
        loadCatalog()
    }

    fun selectCatalogStudio(studio: String) {
        val newStudio = if (_homeState.value.catalogFilter.selectedStudio == studio) "Semua" else studio
        _homeState.update {
            it.copy(catalogFilter = it.catalogFilter.copy(selectedStudio = newStudio, page = 0))
        }
        loadCatalog()
    }

    fun selectCatalogSort(sort: String) {
        _homeState.update {
            it.copy(catalogFilter = it.catalogFilter.copy(selectedSort = sort, page = 0))
        }
        loadCatalog()
    }

    fun loadCatalog() {
        viewModelScope.launch {
            _homeState.update { it.copy(isLoading = true, errorMessage = null) }
            val filter = _homeState.value.catalogFilter
            val cleanGenre = if (filter.selectedGenre == "Semua") null else filter.selectedGenre
            val cleanStudio = if (filter.selectedStudio == "Semua") null else filter.selectedStudio

            val result = repository.searchAnime(
                keyword = "",
                genre = cleanGenre,
                studio = cleanStudio,
                sort = filter.selectedSort,
                page = filter.page
            )
            result.onSuccess { list ->
                _homeState.update {
                    it.copy(
                        isLoading = false,
                        catalogList = list,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _homeState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal memuat katalog: ${error.localizedMessage ?: "Koneksi terputus"}"
                    )
                }
            }
        }
    }

    // --- SEARCH METHODS (BY TITLE, GENRE, STUDIO) ---
    fun onSearchQueryChanged(query: String) {
        _homeState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400) // Debounce
            executeSearch()
        }
    }

    fun selectSearchGenre(genre: String) {
        val newGenre = if (_homeState.value.searchGenre == genre) "Semua" else genre
        _homeState.update { it.copy(searchGenre = newGenre) }
        executeSearch()
    }

    fun selectSearchStudio(studio: String) {
        val newStudio = if (_homeState.value.searchStudio == studio) "Semua" else studio
        _homeState.update { it.copy(searchStudio = newStudio) }
        executeSearch()
    }

    fun executeSearch() {
        val query = _homeState.value.searchQuery.trim()
        val genre = _homeState.value.searchGenre
        val studio = _homeState.value.searchStudio

        val cleanGenre = if (genre == "Semua") null else genre
        val cleanStudio = if (studio == "Semua") null else studio

        val effectiveKeyword = if (query.isBlank() && cleanGenre == null && cleanStudio == null) {
            "one piece"
        } else {
            query
        }

        viewModelScope.launch {
            _homeState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.searchAnime(
                keyword = effectiveKeyword,
                genre = cleanGenre,
                studio = cleanStudio
            )
            result.onSuccess { list ->
                _homeState.update {
                    it.copy(
                        isLoading = false,
                        animeList = list,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _homeState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Pencarian gagal: ${error.localizedMessage ?: "Server tidak merespons"}. Silakan periksa jaringan dan coba lagi."
                    )
                }
            }
        }
    }

    fun searchAnime(query: String) {
        _homeState.update { it.copy(searchQuery = query) }
        executeSearch()
    }

    fun openAnimeDetail(anime: AnimeItem) {
        _detailState.update {
            DetailUiState(
                isLoading = true,
                anime = anime,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            val detailResult = repository.getAnimeDetail(anime.id)
            var detailResp: AnimeDetailData? = null
            detailResult.onSuccess { detail ->
                detailResp = detail
            }

            val episodesResult = repository.getAnimeEpisodes(anime.id)
            episodesResult.onSuccess { eps ->
                val finalEpisodes = if (eps.isNotEmpty()) {
                    eps
                } else {
                    detailResp?.episode ?: emptyList()
                }
                _detailState.update {
                    it.copy(
                        isLoading = false,
                        detail = detailResp,
                        episodes = finalEpisodes,
                        errorMessage = if (finalEpisodes.isEmpty()) "Belum ada episode yang tersedia untuk anime ini." else null
                    )
                }
            }.onFailure { error ->
                val existingEps = detailResp?.episode ?: emptyList()
                if (existingEps.isNotEmpty()) {
                    _detailState.update {
                        it.copy(
                            isLoading = false,
                            detail = detailResp,
                            episodes = existingEps,
                            errorMessage = null
                        )
                    }
                } else {
                    _detailState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Gagal mengambil detail episode: ${error.localizedMessage ?: "Koneksi terputus"}"
                        )
                    }
                }
            }
        }
    }

    fun playEpisode(episode: AnimeEpisodeItem, allEpisodes: List<AnimeEpisodeItem>, animeTitle: String) {
        _playerState.update {
            PlayerUiState(
                isLoading = true,
                animeTitle = animeTitle,
                episodes = allEpisodes,
                currentEpisode = episode,
                errorMessage = null,
                savedPositionMs = 0L
            )
        }

        viewModelScope.launch {
            val result = repository.getEpisodeStream(episode.id)
            result.onSuccess { stream ->
                val downloads = stream.download
                val defaultDownload = downloads.firstOrNull { it.quality?.contains("720") == true }
                    ?: downloads.firstOrNull()

                val resolvedUrl = defaultDownload?.link ?: ""

                _playerState.update {
                    it.copy(
                        isLoading = false,
                        streamData = stream,
                        selectedQuality = defaultDownload,
                        activeStreamUrl = resolvedUrl,
                        errorMessage = if (downloads.isEmpty()) "Tidak ada link streaming untuk episode ini." else null
                    )
                }
            }.onFailure { error ->
                _playerState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal mengambil stream video: ${error.localizedMessage ?: "Server tidak merespons"}"
                    )
                }
            }
        }
    }

    fun selectQuality(downloadItem: StreamDownloadItem) {
        _playerState.update {
            it.copy(
                selectedQuality = downloadItem,
                activeStreamUrl = downloadItem.link
            )
        }
    }

    fun updatePlaybackPosition(positionMs: Long) {
        if (positionMs > 0L) {
            _playerState.update { it.copy(savedPositionMs = positionMs) }
        }
    }

    fun resetPlaybackPosition() {
        _playerState.update { it.copy(savedPositionMs = 0L) }
    }

    fun nextEpisode() {
        val current = _playerState.value.currentEpisode ?: return
        val list = _playerState.value.episodes
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex in 0 until (list.size - 1)) {
            val nextEp = list[currentIndex + 1]
            playEpisode(nextEp, list, _playerState.value.animeTitle)
        }
    }

    fun prevEpisode() {
        val current = _playerState.value.currentEpisode ?: return
        val list = _playerState.value.episodes
        val currentIndex = list.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            val prevEp = list[currentIndex - 1]
            playEpisode(prevEp, list, _playerState.value.animeTitle)
        }
    }

    fun retryHome() {
        if (_homeState.value.activeTab == HomeTab.SCHEDULE) {
            loadSchedule(_homeState.value.selectedDay)
        } else {
            searchAnime(_homeState.value.searchQuery.ifBlank { "one piece" })
        }
    }

    fun retryDetail() {
        _detailState.value.anime?.let { openAnimeDetail(it) }
    }

    fun retryPlayer() {
        val curr = _playerState.value.currentEpisode
        val list = _playerState.value.episodes
        val title = _playerState.value.animeTitle
        if (curr != null) {
            playEpisode(curr, list, title)
        }
    }
}
