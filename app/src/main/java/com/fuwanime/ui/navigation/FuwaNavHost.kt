package com.fuwanime.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fuwanime.data.Anime
import com.fuwanime.data.Episode
import com.fuwanime.ui.detail.DetailRoute
import com.fuwanime.ui.explore.ExploreRoute
import com.fuwanime.ui.home.HomeRoute
import com.fuwanime.ui.library.LibraryRoute
import com.fuwanime.ui.player.PlayerRoute
import com.fuwanime.ui.schedule.ScheduleRoute
import com.fuwanime.ui.settings.SettingsRoute

private object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore?genre={genre}&focus={focus}"
    const val SCHEDULE = "schedule"
    const val LIBRARY = "library"
    const val DETAIL = "detail/{animeId}"
    const val PLAYER = "player/{animeId}/{episodeId}?number={number}"
    const val SETTINGS = "settings"

    fun explore(genre: String? = null, focus: Boolean = false) =
        "explore?genre=${Uri.encode(genre.orEmpty())}&focus=$focus"

    fun detail(animeId: String) = "detail/${Uri.encode(animeId)}"
    fun player(animeId: String, episodeId: String, number: String) =
        "player/${Uri.encode(animeId)}/${Uri.encode(episodeId)}?number=${Uri.encode(number)}"
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME(Routes.HOME, "Beranda", Icons.Outlined.Home, Icons.Rounded.Home),
    EXPLORE(Routes.EXPLORE, "Jelajah", Icons.Outlined.Explore, Icons.Rounded.Explore),
    SCHEDULE(Routes.SCHEDULE, "Jadwal", Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth),
    LIBRARY(Routes.LIBRARY, "Koleksi", Icons.Outlined.VideoLibrary, Icons.Rounded.VideoLibrary),
}

private fun NavHostController.navigateTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun FuwaNavHost(initialAnimeId: String? = null, onInitialAnimeConsumed: () -> Unit = {}) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val currentTab = Tab.entries.firstOrNull { tab -> destination?.hierarchy?.any { it.route == tab.route } == true }

    // Deep link from a "new episode" notification: jump straight to the detail screen.
    LaunchedEffect(initialAnimeId) {
        if (initialAnimeId != null) {
            nav.navigate(Routes.detail(initialAnimeId))
            onInitialAnimeConsumed()
        }
    }

    val openAnime: (Anime) -> Unit = { nav.navigate(Routes.detail(it.id)) }
    val play: (String, Episode) -> Unit = { animeId, ep -> nav.navigate(Routes.player(animeId, ep.id, ep.number)) }
    // Fresh Explore entry (no restoreState) so the new genre/focus arguments take effect.
    val openExplore: (String?, Boolean) -> Unit = { genre, focus ->
        nav.navigate(Routes.explore(genre, focus)) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = currentTab != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Tab.entries.forEach { tab ->
                        val selected = tab == currentTab
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (tab == Tab.EXPLORE) nav.navigateTab(Routes.explore()) else nav.navigateTab(tab.route)
                            },
                            icon = { Icon(if (selected) tab.selectedIcon else tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(180)) },
            ) {
                composable(Routes.HOME) {
                    HomeRoute(
                        onAnimeClick = openAnime,
                        onContinue = { e -> nav.navigate(Routes.player(e.animeId, e.episodeId, e.episodeNumber)) },
                        onSearch = { openExplore(null, true) },
                        onSettings = { nav.navigate(Routes.SETTINGS) },
                        onSchedule = { nav.navigateTab(Routes.SCHEDULE) },
                        onExplore = { g -> openExplore(g, false) },
                    )
                }
                composable(
                    Routes.EXPLORE,
                    arguments = listOf(
                        navArgument("genre") { type = NavType.StringType; defaultValue = "" },
                        navArgument("focus") { type = NavType.BoolType; defaultValue = false },
                    ),
                ) { entry ->
                    ExploreRoute(focusSearch = entry.arguments?.getBoolean("focus") == true, onAnimeClick = openAnime)
                }
                composable(Routes.SCHEDULE) { ScheduleRoute(onAnimeClick = openAnime) }
                composable(Routes.LIBRARY) {
                    LibraryRoute(
                        onAnimeClick = openAnime,
                        onResume = { e -> nav.navigate(Routes.player(e.animeId, e.episodeId, e.episodeNumber)) },
                        onPlayDownload = { d -> nav.navigate(Routes.player(d.animeId, d.episodeId, d.episodeNumber)) },
                    )
                }
                composable(
                    Routes.DETAIL,
                    arguments = listOf(navArgument("animeId") { type = NavType.StringType }),
                    enterTransition = { slideInHorizontally(tween(280)) { it / 4 } + fadeIn(tween(280)) },
                    popExitTransition = { slideOutHorizontally(tween(220)) { it / 4 } + fadeOut(tween(220)) },
                ) {
                    DetailRoute(
                        onBack = { nav.popBackStack() },
                        onPlay = { anime, ep -> play(anime.id, ep) },
                        onAnimeClick = openAnime,
                        onGenre = { g -> openExplore(g, false) },
                    )
                }
                composable(
                    Routes.PLAYER,
                    arguments = listOf(
                        navArgument("animeId") { type = NavType.StringType },
                        navArgument("episodeId") { type = NavType.StringType },
                        navArgument("number") { type = NavType.StringType; defaultValue = "" },
                    ),
                    enterTransition = { slideInVertically(tween(280)) { it / 3 } + fadeIn(tween(280)) },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { slideOutVertically(tween(220)) { it / 3 } + fadeOut(tween(220)) },
                ) {
                    PlayerRoute(onBack = { nav.popBackStack() })
                }
                composable(Routes.SETTINGS) { SettingsRoute(onBack = { nav.popBackStack() }) }
            }
        }
    }
}
