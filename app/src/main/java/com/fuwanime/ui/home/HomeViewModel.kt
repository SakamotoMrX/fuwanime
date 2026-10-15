package com.fuwanime.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuwanime.data.Anime
import com.fuwanime.data.AnimeRepository
import com.fuwanime.data.LibraryRepository
import com.fuwanime.data.SortOrder
import com.fuwanime.data.local.WatchHistoryEntity
import com.fuwanime.util.Day
import com.fuwanime.util.friendlyMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeFeed(
    val popular: List<Anime> = emptyList(),
    val today: List<Anime> = emptyList(),
    val thisSeason: List<Anime> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
)

data class HomeUiState(
    val feed: HomeFeed = HomeFeed(),
    val continueWatching: List<WatchHistoryEntity> = emptyList(),
) {
    val hero: List<Anime> get() = (feed.thisSeason.sortedByDescending { it.views }.take(3) + feed.popular)
        .distinctBy { it.id }
        .filter { it.backdrop.isNotBlank() }
        .take(6)
}

class HomeViewModel(
    private val anime: AnimeRepository,
    library: LibraryRepository,
) : ViewModel() {

    private val feed = MutableStateFlow(HomeFeed())

    val state: StateFlow<HomeUiState> = combine(feed, library.continueWatching(15)) { f, cw ->
        HomeUiState(f, cw)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val today: Day = Day.today()

    init {
        load(refreshing = false)
    }

    fun refresh() = load(refreshing = true)

    private fun load(refreshing: Boolean) {
        feed.update { it.copy(loading = !refreshing, refreshing = refreshing, error = null) }
        viewModelScope.launch {
            val popularJob = async { anime.explore(page = 0, sort = SortOrder.POPULAR) }
            // Every day's schedule together approximates "currently airing this season".
            val dayJobs = Day.entries.map { day -> day to async { anime.schedule(day.apiValue) } }

            val popular = popularJob.await()
            val days = dayJobs.map { (day, job) -> day to job.await() }

            val todayList = days.first { it.first == today }.second.getOrDefault(emptyList())
            val season = days.flatMap { it.second.getOrDefault(emptyList()) }
                .distinctBy { it.id }
                .sortedByDescending { it.views }

            val error = popular.exceptionOrNull()?.takeIf { season.isEmpty() }?.friendlyMessage()
            feed.value = HomeFeed(
                popular = popular.getOrDefault(feed.value.popular),
                today = todayList,
                thisSeason = season,
                loading = false,
                refreshing = false,
                error = error,
            )
        }
    }
}
