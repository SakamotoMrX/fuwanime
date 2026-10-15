package com.fuwanime.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fuwanime.data.Anime
import com.fuwanime.data.LibraryRepository
import com.fuwanime.data.local.FavoriteEntity
import com.fuwanime.data.local.WatchHistoryEntity
import com.fuwanime.ui.components.MessageState
import com.fuwanime.ui.components.NetImage
import com.fuwanime.ui.components.PosterCard
import com.fuwanime.util.formatDuration
import com.fuwanime.util.fuwaViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class LibraryViewModel(private val library: LibraryRepository) : ViewModel() {
    val history: StateFlow<List<WatchHistoryEntity>?> =
        library.continueWatching(500).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val favorites: StateFlow<List<FavoriteEntity>?> =
        library.favoriteList.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun removeHistory(animeId: String) = viewModelScope.launch { library.removeFromHistory(animeId) }
    fun clearHistory() = viewModelScope.launch { library.clearHistory() }
}

fun FavoriteEntity.toAnime() = Anime(
    id = animeId,
    title = title,
    poster = poster,
    cover = cover,
    type = type,
    status = status,
    year = year,
    genres = genres.split(',').filter { it.isNotBlank() },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryRoute(onAnimeClick: (Anime) -> Unit, onResume: (WatchHistoryEntity) -> Unit) {
    val vm: LibraryViewModel = fuwaViewModel { c, _ -> LibraryViewModel(c.libraryRepository) }
    val history by vm.history.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Koleksi", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            if (pager.currentPage == 0 && !history.isNullOrEmpty()) {
                IconButton(onClick = { confirmClear = true }) { Icon(Icons.Rounded.DeleteSweep, "Hapus riwayat") }
            }
        }
        PrimaryTabRow(selectedTabIndex = pager.currentPage) {
            listOf("Riwayat" to history?.size, "Favorit" to favorites?.size).forEachIndexed { i, (label, count) ->
                Tab(
                    selected = pager.currentPage == i,
                    onClick = { scope.launch { pager.animateScrollToPage(i) } },
                    text = { Text(if (count != null && count > 0) "$label ($count)" else label) },
                )
            }
        }
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> HistoryList(history, onResume, onAnimeClick, onRemove = { vm.removeHistory(it) })
                else -> FavoriteGrid(favorites, onAnimeClick)
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, null) },
            title = { Text("Hapus semua riwayat?") },
            text = { Text("Progres tontonan semua anime akan dihapus. Favorit tidak terpengaruh.") },
            confirmButton = { TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun HistoryList(
    items: List<WatchHistoryEntity>?,
    onResume: (WatchHistoryEntity) -> Unit,
    onAnimeClick: (Anime) -> Unit,
    onRemove: (String) -> Unit,
) {
    if (items == null) return
    if (items.isEmpty()) {
        MessageState(Icons.Rounded.History, "Belum ada riwayat", message = "Episode yang kamu tonton akan muncul di sini.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.episodeId }) { entry ->
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { onResume(entry) }.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(140.dp).aspectRatio(16f / 9f).clip(MaterialTheme.shapes.small)) {
                    NetImage(entry.episodeThumbnail.ifBlank { entry.animeCover }, Modifier.fillMaxSize())
                    LinearProgressIndicator(
                        progress = { entry.progress },
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
                        drawStopIndicator = {},
                        gapSize = 0.dp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.animeTitle,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            onAnimeClick(Anime(entry.animeId, entry.animeTitle, poster = entry.animePoster, cover = entry.animeCover))
                        },
                    )
                    Text("Episode ${entry.episodeNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        if (entry.isFinished) "Selesai ditonton" else "${formatDuration(entry.positionMs)} / ${formatDuration(entry.durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.updatedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                IconButton(onClick = { onRemove(entry.animeId) }) { Icon(Icons.Rounded.Close, "Hapus dari riwayat") }
            }
        }
    }
}

@Composable
private fun FavoriteGrid(items: List<FavoriteEntity>?, onAnimeClick: (Anime) -> Unit) {
    if (items == null) return
    if (items.isEmpty()) {
        MessageState(Icons.Rounded.FavoriteBorder, "Belum ada favorit", message = "Ketuk ikon hati di halaman anime untuk menyimpannya di sini.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(108.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(items, key = { it.animeId }) { fav ->
            val anime = fav.toAnime()
            PosterCard(anime, onClick = { onAnimeClick(anime) })
        }
    }
}
