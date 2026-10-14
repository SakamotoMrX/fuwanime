package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnimeItem
import com.example.ui.components.AnimeGridSkeleton
import com.example.ui.components.AnimePosterCard
import com.example.ui.components.AnimeSearchBar
import com.example.ui.components.DayChipsRow
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.FilterChipsRow
import com.example.ui.theme.FuwaDarkBg
import com.example.ui.theme.FuwaDarkSurface
import com.example.ui.theme.FuwaDarkSurfaceContainer
import com.example.ui.theme.FuwaDarkSurfaceContainerHigh
import com.example.ui.theme.FuwaPrimary
import com.example.ui.theme.FuwaSecondary
import com.example.ui.theme.FuwaTertiary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AnimeViewModel
import com.example.ui.viewmodel.HomeTab
import com.example.ui.viewmodel.HomeUiState

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    viewModel: AnimeViewModel,
    onAnimeClick: (AnimeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FuwaDarkBg)
            .statusBarsPadding()
    ) {
        // 1. FuwaNime Material 3 App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(FuwaPrimary, FuwaSecondary)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "FuwaNime Icon",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Fuwa",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "Nime",
                            color = FuwaSecondary,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }
                    Text(
                        text = "Katalog & Streaming Anime Bebas Iklan",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 2. Active Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (uiState.activeTab) {
                HomeTab.SCHEDULE -> {
                    ScheduleTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onAnimeClick = onAnimeClick
                    )
                }
                HomeTab.CATALOG -> {
                    CatalogTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onAnimeClick = onAnimeClick
                    )
                }
                HomeTab.SEARCH -> {
                    SearchTabContent(
                        uiState = uiState,
                        viewModel = viewModel,
                        onAnimeClick = onAnimeClick
                    )
                }
            }
        }

        // 3. Material 3 Bottom Navigation Bar
        NavigationBar(
            containerColor = FuwaDarkSurface,
            contentColor = TextPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            NavigationBarItem(
                selected = uiState.activeTab == HomeTab.SCHEDULE,
                onClick = { viewModel.switchTab(HomeTab.SCHEDULE) },
                icon = {
                    Icon(
                        imageVector = if (uiState.activeTab == HomeTab.SCHEDULE) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                        contentDescription = "Jadwal"
                    )
                },
                label = { Text("Jadwal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = FuwaPrimary,
                    indicatorColor = FuwaPrimary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                ),
                modifier = Modifier.testTag("nav_schedule")
            )

            NavigationBarItem(
                selected = uiState.activeTab == HomeTab.CATALOG,
                onClick = { viewModel.switchTab(HomeTab.CATALOG) },
                icon = {
                    Icon(
                        imageVector = if (uiState.activeTab == HomeTab.CATALOG) Icons.Default.Explore else Icons.Outlined.Explore,
                        contentDescription = "Katalog"
                    )
                },
                label = { Text("Katalog", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = FuwaPrimary,
                    indicatorColor = FuwaPrimary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                ),
                modifier = Modifier.testTag("nav_catalog")
            )

            NavigationBarItem(
                selected = uiState.activeTab == HomeTab.SEARCH,
                onClick = { viewModel.switchTab(HomeTab.SEARCH) },
                icon = {
                    Icon(
                        imageVector = if (uiState.activeTab == HomeTab.SEARCH) Icons.Default.Search else Icons.Outlined.Search,
                        contentDescription = "Cari"
                    )
                },
                label = { Text("Pencarian", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = FuwaPrimary,
                    indicatorColor = FuwaPrimary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                ),
                modifier = Modifier.testTag("nav_search")
            )
        }
    }
}

@Composable
private fun ScheduleTabContent(
    uiState: HomeUiState,
    viewModel: AnimeViewModel,
    onAnimeClick: (AnimeItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DayChipsRow(
            days = viewModel.daysOfWeek,
            selectedDay = uiState.selectedDay,
            onDaySelected = { viewModel.selectDay(it) },
            modifier = Modifier.padding(vertical = 4.dp)
        )

        AnimeListOrState(
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            animeList = uiState.animeList,
            emptyMessage = "Tidak ada jadwal tayang untuk hari ${uiState.selectedDay.replaceFirstChar { it.uppercase() }}.",
            onRetry = { viewModel.retryHome() },
            onAnimeClick = onAnimeClick
        )
    }
}

@Composable
private fun CatalogTabContent(
    uiState: HomeUiState,
    viewModel: AnimeViewModel,
    onAnimeClick: (AnimeItem) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val currentSortLabel = viewModel.sortOptions.firstOrNull { it.first == uiState.catalogFilter.selectedSort }?.second ?: "Terpopuler"

    Column(modifier = Modifier.fillMaxSize()) {
        // Sort & Active Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = FuwaSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Eksplor Katalog",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (uiState.catalogFilter.selectedGenre != "Semua" || uiState.catalogFilter.selectedStudio != "Semua") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.selectCatalogGenre("Semua")
                                viewModel.selectCatalogStudio("Semua")
                            }
                            .testTag("reset_catalog_filter_btn"),
                        color = FuwaPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Reset",
                            color = FuwaSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Sort Selector Button
            Box {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { sortMenuExpanded = true }
                        .testTag("catalog_sort_btn"),
                    color = FuwaDarkSurfaceContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FuwaPrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = null,
                            tint = FuwaPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentSortLabel,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier.background(FuwaDarkSurfaceContainerHigh)
                ) {
                    viewModel.sortOptions.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    color = if (key == uiState.catalogFilter.selectedSort) FuwaPrimary else TextPrimary,
                                    fontWeight = if (key == uiState.catalogFilter.selectedSort) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                sortMenuExpanded = false
                                viewModel.selectCatalogSort(key)
                            }
                        )
                    }
                }
            }
        }

        // Genre Filter Chips
        FilterChipsRow(
            title = "Filter Genre",
            items = viewModel.genres,
            selectedItem = uiState.catalogFilter.selectedGenre,
            onItemSelected = { viewModel.selectCatalogGenre(it) },
            icon = Icons.Default.Category
        )

        // Studio Filter Chips
        FilterChipsRow(
            title = "Filter Studio",
            items = viewModel.studios,
            selectedItem = uiState.catalogFilter.selectedStudio,
            onItemSelected = { viewModel.selectCatalogStudio(it) },
            icon = Icons.Default.Business
        )

        Spacer(modifier = Modifier.height(4.dp))

        AnimeListOrState(
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            animeList = uiState.catalogList,
            emptyMessage = "Tidak ada anime ditemukan dengan filter ${uiState.catalogFilter.selectedGenre} / ${uiState.catalogFilter.selectedStudio}.",
            onRetry = { viewModel.loadCatalog() },
            onAnimeClick = onAnimeClick
        )
    }
}

@Composable
private fun SearchTabContent(
    uiState: HomeUiState,
    viewModel: AnimeViewModel,
    onAnimeClick: (AnimeItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search Input
        AnimeSearchBar(
            query = uiState.searchQuery,
            onQueryChange = { viewModel.onSearchQueryChanged(it) },
            placeholderText = "Cari judul anime (One Piece, Jujutsu Kaisen)...",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Quick Genre Search Chips
        FilterChipsRow(
            title = "Cari Berdasarkan Genre",
            items = viewModel.genres,
            selectedItem = uiState.searchGenre,
            onItemSelected = { viewModel.selectSearchGenre(it) },
            icon = Icons.Default.Category
        )

        // Quick Studio Search Chips
        FilterChipsRow(
            title = "Cari Berdasarkan Studio",
            items = viewModel.studios,
            selectedItem = uiState.searchStudio,
            onItemSelected = { viewModel.selectSearchStudio(it) },
            icon = Icons.Default.Business
        )

        Spacer(modifier = Modifier.height(4.dp))

        AnimeListOrState(
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            animeList = uiState.animeList,
            emptyMessage = "Anime '${uiState.searchQuery.ifBlank { uiState.searchGenre }}' tidak ditemukan.",
            onRetry = { viewModel.executeSearch() },
            onAnimeClick = onAnimeClick
        )
    }
}

@Composable
private fun AnimeListOrState(
    isLoading: Boolean,
    errorMessage: String?,
    animeList: List<AnimeItem>,
    emptyMessage: String,
    onRetry: () -> Unit,
    onAnimeClick: (AnimeItem) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                AnimeGridSkeleton()
            }

            errorMessage != null -> {
                ErrorStateCard(
                    message = errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            animeList.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(FuwaDarkSurfaceContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = FuwaPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tidak Ada Hasil",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = emptyMessage,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("anime_grid"),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(animeList, key = { it.id }) { anime ->
                        AnimePosterCard(
                            anime = anime,
                            onClick = { onAnimeClick(anime) }
                        )
                    }
                }
            }
        }
    }
}
