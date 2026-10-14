package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AnimeItem
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.theme.FuwaDarkBg
import com.example.ui.theme.FuwaNimeTheme
import com.example.ui.viewmodel.AnimeViewModel

sealed interface AppScreen {
    data object Home : AppScreen
    data class Detail(val anime: AnimeItem) : AppScreen
    data object Player : AppScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FuwaNimeTheme {
                AnimeApp()
            }
        }
    }
}

@Composable
fun AnimeApp(
    viewModel: AnimeViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        containerColor = FuwaDarkBg
    ) { _ ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                is AppScreen.Home -> {
                    HomeScreen(
                        uiState = homeState,
                        viewModel = viewModel,
                        onAnimeClick = { anime ->
                            viewModel.openAnimeDetail(anime)
                            currentScreen = AppScreen.Detail(anime)
                        }
                    )
                }

                is AppScreen.Detail -> {
                    DetailScreen(
                        uiState = detailState,
                        onBack = { currentScreen = AppScreen.Home },
                        onPlayEpisode = { episode, allEpisodes, animeTitle ->
                            viewModel.playEpisode(episode, allEpisodes, animeTitle)
                            currentScreen = AppScreen.Player
                        },
                        onRetry = { viewModel.retryDetail() }
                    )
                }

                is AppScreen.Player -> {
                    PlayerScreen(
                        uiState = playerState,
                        onBack = {
                            val selected = detailState.anime
                            if (selected != null) {
                                currentScreen = AppScreen.Detail(selected)
                            } else {
                                currentScreen = AppScreen.Home
                            }
                        },
                        onNextEpisode = { viewModel.nextEpisode() },
                        onPrevEpisode = { viewModel.prevEpisode() },
                        onSelectQuality = { viewModel.selectQuality(it) },
                        onSelectEpisode = { ep ->
                            viewModel.playEpisode(ep, playerState.episodes, playerState.animeTitle)
                        },
                        onRetry = { viewModel.retryPlayer() },
                        onUpdatePosition = { pos -> viewModel.updatePlaybackPosition(pos) }
                    )
                }
            }
        }
    }
}
