package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.AnimeEpisodeItem
import com.example.data.model.StreamDownloadItem
import com.example.ui.theme.AnimeCyan
import com.example.ui.theme.AnimeDarkBg
import com.example.ui.theme.AnimeDarkCard
import com.example.ui.theme.AnimeDarkSurface
import com.example.ui.theme.AnimeDarkSurfaceVariant
import com.example.ui.theme.AnimePurple
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PlayerUiState
import kotlinx.coroutines.delay

private fun formatTimeMs(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        String.format("%02d:%02d:%02d", hours, remainingMinutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onBack: () -> Unit,
    onNextEpisode: () -> Unit,
    onPrevEpisode: () -> Unit,
    onSelectQuality: (StreamDownloadItem) -> Unit,
    onSelectEpisode: (AnimeEpisodeItem) -> Unit,
    onRetry: () -> Unit,
    onUpdatePosition: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var isVideoBuffering by remember { mutableStateOf(true) }
    var videoError by remember { mutableStateOf<String?>(null) }
    var qualityMenuExpanded by remember { mutableStateOf(false) }
    var speedMenuExpanded by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by rememberSaveable {
        mutableLongStateOf(if (uiState.savedPositionMs > 0L) uiState.savedPositionMs else 0L)
    }
    var totalDurationMs by rememberSaveable { mutableLongStateOf(0L) }
    var savedPlaybackPositionMs by rememberSaveable {
        mutableLongStateOf(if (uiState.savedPositionMs > 0L) uiState.savedPositionMs else 0L)
    }
    var lastLoadedUrl by rememberSaveable { mutableStateOf("") }
    var lastEpisodeId by rememberSaveable { mutableStateOf(uiState.currentEpisode?.id ?: "") }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragPosition by remember { mutableFloatStateOf(0f) }

    var showOverlayControls by remember { mutableStateOf(true) }
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerInstance by remember { mutableStateOf<MediaPlayer?>(null) }

    // Helper to capture current playback position safely before any transition
    fun captureCurrentPosition() {
        val vv = videoViewInstance ?: return
        try {
            val pos = vv.currentPosition.toLong()
            if (pos > 0L) {
                savedPlaybackPositionMs = pos
                currentPositionMs = pos
                onUpdatePosition(pos)
            }
        } catch (_: Exception) {}
    }

    // Detect when episode changes to reset playback position to 00:00
    LaunchedEffect(uiState.currentEpisode?.id) {
        val currentId = uiState.currentEpisode?.id ?: ""
        if (currentId.isNotBlank() && currentId != lastEpisodeId) {
            lastEpisodeId = currentId
            savedPlaybackPositionMs = 0L
            currentPositionMs = 0L
            onUpdatePosition(0L)
        }
    }

    // Intercept back button to escape fullscreen first
    BackHandler {
        if (isFullscreen) {
            captureCurrentPosition()
            isFullscreen = false
        } else {
            onBack()
        }
    }

    // Toggle orientation based on fullscreen state
    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Continuously update position and keep saved position fresh
    LaunchedEffect(videoViewInstance, isPlaying, isDraggingSlider) {
        while (true) {
            delay(500)
            val vv = videoViewInstance
            if (vv != null && !isDraggingSlider) {
                try {
                    val pos = vv.currentPosition.toLong()
                    val dur = vv.duration.toLong()
                    // CRITICAL: NEVER overwrite savedPlaybackPositionMs if pos is 0 or negative!
                    // During fullscreen transition or surface recreation, currentPosition briefly returns 0.
                    if (pos > 0L) {
                        currentPositionMs = pos
                        savedPlaybackPositionMs = pos
                        onUpdatePosition(pos)
                    }
                    if (dur > 0L) {
                        totalDurationMs = dur
                    }
                    isPlaying = vv.isPlaying
                } catch (_: Exception) {}
            }
        }
    }

    // Auto-hide controls in fullscreen after 5 seconds of inactivity
    LaunchedEffect(showOverlayControls, isFullscreen, isPlaying) {
        if (isFullscreen && showOverlayControls && isPlaying) {
            delay(5000)
            showOverlayControls = false
        }
    }

    val currentEpisode = uiState.currentEpisode
    val episodes = uiState.episodes
    val currentIndex = episodes.indexOfFirst { it.id == currentEpisode?.id }
    val hasPrev = currentIndex > 0
    val hasNext = currentIndex in 0 until (episodes.size - 1)

    fun seekToPosition(ms: Long) {
        val target = ms.coerceIn(0L, if (totalDurationMs > 0L) totalDurationMs else Long.MAX_VALUE)
        videoViewInstance?.seekTo(target.toInt())
        currentPositionMs = target
        savedPlaybackPositionMs = target
        onUpdatePosition(target)
    }

    fun togglePlayPause() {
        val vv = videoViewInstance ?: return
        if (vv.isPlaying) {
            vv.pause()
            isPlaying = false
        } else {
            vv.start()
            isPlaying = true
        }
    }

    fun applySpeed(speed: Float) {
        playbackSpeed = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val mp = mediaPlayerInstance ?: return
                val params = mp.playbackParams
                params.speed = speed
                mp.playbackParams = params
            } catch (_: Exception) {}
        }
    }

    fun handleQualityChange(item: StreamDownloadItem) {
        captureCurrentPosition()
        onSelectQuality(item)
    }

    fun toggleFullscreen(enable: Boolean) {
        captureCurrentPosition()
        isFullscreen = enable
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnimeDarkBg)
            .then(
                if (!isFullscreen) {
                    Modifier
                        .statusBarsPadding()
                        .navigationBarsPadding()
                } else {
                    Modifier
                }
            )
    ) {
        // 1. TOP BAR (Only in Portrait / Non-fullscreen)
        if (!isFullscreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("player_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Detail",
                        tint = TextPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = uiState.animeTitle,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentEpisode?.title ?: "Episode ${currentEpisode?.displayIndex ?: 1}",
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quality Selector
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (uiState.streamData?.download?.isNotEmpty() == true) {
                                    qualityMenuExpanded = true
                                }
                            }
                            .testTag("quality_selector_button"),
                        color = AnimeDarkSurface,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AnimePurple)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = AnimePurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = uiState.selectedQuality?.quality ?: "720p",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = qualityMenuExpanded,
                        onDismissRequest = { qualityMenuExpanded = false },
                        modifier = Modifier.background(AnimeDarkSurface)
                    ) {
                        uiState.streamData?.download?.forEach { item ->
                            val isSelected = item.link == uiState.selectedQuality?.link
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${item.quality ?: "Direct"} - ${item.name ?: "Server"}",
                                            color = if (isSelected) AnimePurple else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = AnimePurple,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    qualityMenuExpanded = false
                                    handleQualityChange(item)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Fullscreen Button
                IconButton(
                    onClick = { toggleFullscreen(true) },
                    modifier = Modifier.testTag("fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Layar Penuh (Fullscreen)",
                        tint = TextPrimary
                    )
                }
            }
        }

        // 2. VIDEO PLAYER BOX (Single continuous node, resizes seamlessly without recreating VideoView)
        Box(
            modifier = if (isFullscreen) {
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .testTag("fullscreen_player_container")
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showOverlayControls = !showOverlayControls
                    }
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
                    .testTag("video_player_box")
            }
        ) {
            // SINGLE ANDROIDVIEW: Never recreated on fullscreen/esc toggle!
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setOnPreparedListener { mp ->
                            mediaPlayerInstance = mp
                            isVideoBuffering = false
                            videoError = null
                            if (duration > 0) {
                                totalDurationMs = duration.toLong()
                            }
                            val targetPos = if (savedPlaybackPositionMs > 0L) {
                                savedPlaybackPositionMs
                            } else {
                                uiState.savedPositionMs
                            }
                            if (targetPos > 0L) {
                                seekTo(targetPos.toInt())
                                currentPositionMs = targetPos
                            }
                            mp.isLooping = false
                            mp.start()
                            isPlaying = true
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            savedPlaybackPositionMs = 0L
                            currentPositionMs = totalDurationMs
                            onUpdatePosition(0L)
                        }
                        setOnErrorListener { _, what, extra ->
                            isVideoBuffering = false
                            videoError = "Gagal memutar video ($what, $extra). Coba pilih server lain."
                            true
                        }
                        videoViewInstance = this
                        if (uiState.activeStreamUrl.isNotBlank()) {
                            lastLoadedUrl = uiState.activeStreamUrl
                            setVideoURI(Uri.parse(uiState.activeStreamUrl))
                        }
                    }
                },
                update = { vView ->
                    videoViewInstance = vView
                    // CRITICAL: Only setVideoURI if the URL has actually changed to a new stream!
                    // This prevents resetting the video to 00:00 when fullscreen toggles or recompositions occur!
                    if (uiState.activeStreamUrl.isNotBlank() && uiState.activeStreamUrl != lastLoadedUrl) {
                        lastLoadedUrl = uiState.activeStreamUrl
                        vView.setVideoURI(Uri.parse(uiState.activeStreamUrl))
                        vView.start()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Buffering Indicator
            if (isVideoBuffering && videoError == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AnimePurple, strokeWidth = 3.dp)
                }
            }

            // Error Overlay
            if (videoError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(text = videoError ?: "Error memutar", color = TextPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                videoError = null
                                isVideoBuffering = true
                                videoViewInstance?.setVideoURI(Uri.parse(uiState.activeStreamUrl))
                                videoViewInstance?.start()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AnimePurple)
                        ) {
                            Text("Putar Ulang")
                        }
                    }
                }
            }

            // OVERLAYS DEPENDING ON MODE:
            if (isFullscreen) {
                // --- FULLSCREEN HUD OVERLAY (Inspired by Reference Screenshot) ---
                if (showOverlayControls) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Fullscreen Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                                    )
                                )
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { toggleFullscreen(false) },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .size(44.dp)
                                    .testTag("esc_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Keluar Layar Penuh (ESC)",
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.animeTitle,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentEpisode?.title ?: "Episode ${currentEpisode?.displayIndex ?: 1}",
                                    color = AnimeCyan,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Quality Dropdown Menu in Fullscreen
                            Box {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { qualityMenuExpanded = true },
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AnimePurple)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HighQuality,
                                            contentDescription = null,
                                            tint = AnimePurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = uiState.selectedQuality?.quality ?: "720p",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = qualityMenuExpanded,
                                    onDismissRequest = { qualityMenuExpanded = false },
                                    modifier = Modifier.background(AnimeDarkSurface)
                                ) {
                                    uiState.streamData?.download?.forEach { item ->
                                        val isSelected = item.link == uiState.selectedQuality?.link
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${item.quality ?: "Direct"} - ${item.name ?: "Server"}",
                                                    color = if (isSelected) AnimePurple else TextPrimary,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                qualityMenuExpanded = false
                                                handleQualityChange(item)
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Fullscreen ESC Button
                            Button(
                                onClick = { toggleFullscreen(false) },
                                colors = ButtonDefaults.buttonColors(containerColor = AnimePurple),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("exit_fullscreen_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "ESC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Fullscreen Bottom Controls Row
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            // Red Progress Scrub Bar
                            val sliderProgress = if (isDraggingSlider) {
                                sliderDragPosition
                            } else if (totalDurationMs > 0) {
                                (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            Slider(
                                value = sliderProgress,
                                onValueChange = {
                                    isDraggingSlider = true
                                    sliderDragPosition = it
                                },
                                onValueChangeFinished = {
                                    isDraggingSlider = false
                                    val targetMs = (sliderDragPosition * totalDurationMs).toLong()
                                    seekToPosition(targetMs)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFEF4444),
                                    activeTrackColor = Color(0xFFEF4444),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .testTag("fullscreen_seek_slider")
                            )

                            // Controls Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { togglePlayPause() },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Jeda" else "Putar",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { seekToPosition(currentPositionMs - 10000) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "Mundur 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { seekToPosition(currentPositionMs + 10000) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "Maju 10s",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = "${formatTimeMs(currentPositionMs)} / ${formatTimeMs(totalDurationMs)}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = onPrevEpisode,
                                    enabled = hasPrev,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Prev Episode",
                                        tint = if (hasPrev) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = onNextEpisode,
                                    enabled = hasNext,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next Episode",
                                        tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Playback Speed
                                Box {
                                    Text(
                                        text = "${playbackSpeed}x",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { speedMenuExpanded = true }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    )

                                    DropdownMenu(
                                        expanded = speedMenuExpanded,
                                        onDismissRequest = { speedMenuExpanded = false },
                                        modifier = Modifier.background(AnimeDarkSurface)
                                    ) {
                                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "${speed}x",
                                                        color = if (playbackSpeed == speed) AnimePurple else TextPrimary,
                                                        fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    speedMenuExpanded = false
                                                    applySpeed(speed)
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = { qualityMenuExpanded = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Kualitas",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { toggleFullscreen(false) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FullscreenExit,
                                        contentDescription = "Keluar Layar Penuh",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Portrait overlay button in bottom-right corner of video
                IconButton(
                    onClick = { toggleFullscreen(true) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .size(36.dp)
                        .testTag("video_overlay_fullscreen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Layar Penuh",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 3. BOTTOM SECTIONS (Only visible when NOT fullscreen)
        if (!isFullscreen) {
            // Interactive Scrubber Bar in Portrait
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AnimeDarkSurface)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                val sliderProgress = if (isDraggingSlider) {
                    sliderDragPosition
                } else if (totalDurationMs > 0) {
                    (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Slider(
                    value = sliderProgress,
                    onValueChange = {
                        isDraggingSlider = true
                        sliderDragPosition = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        val targetMs = (sliderDragPosition * totalDurationMs).toLong()
                        seekToPosition(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFEF4444),
                        activeTrackColor = Color(0xFFEF4444),
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { togglePlayPause() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { seekToPosition(currentPositionMs - 10000) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Mundur 10s",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { seekToPosition(currentPositionMs + 10000) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Maju 10s",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "${formatTimeMs(currentPositionMs)} / ${formatTimeMs(totalDurationMs)}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${playbackSpeed}x",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { speedMenuExpanded = true }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )

                        IconButton(
                            onClick = { toggleFullscreen(true) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Layar Penuh",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Prev & Next Episode Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPrevEpisode,
                    enabled = hasPrev,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AnimeDarkSurface,
                        contentColor = TextPrimary,
                        disabledContainerColor = AnimeDarkSurface.copy(alpha = 0.4f),
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("prev_episode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Prev Ep", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onNextEpisode,
                    enabled = hasNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AnimePurple,
                        contentColor = Color.White,
                        disabledContainerColor = AnimePurple.copy(alpha = 0.3f),
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("next_episode_button")
                ) {
                    Text(text = "Next Ep", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Scrollable Content Area: Quality chips & Episode carousel
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                // Quality Options Bar
                if (uiState.streamData?.download?.isNotEmpty() == true) {
                    Text(
                        text = "Pilihan Kualitas Video:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.streamData.download.forEach { item ->
                            val isSelected = item.link == uiState.selectedQuality?.link
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { handleQualityChange(item) }
                                    .testTag("quality_chip_${item.quality ?: "default"}"),
                                color = if (isSelected) AnimePurple else AnimeDarkSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, AnimeDarkSurfaceVariant)
                            ) {
                                Text(
                                    text = item.quality ?: "Direct",
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Quick Episodes Carousel
                Text(
                    text = "Pilih Episode Lain (${episodes.size} Total):",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    episodes.forEach { ep ->
                        val isPlayingEp = ep.id == currentEpisode?.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectEpisode(ep) }
                                .testTag("quick_ep_${ep.displayIndex}"),
                            color = if (isPlayingEp) AnimePurple else AnimeDarkCard,
                            shape = RoundedCornerShape(10.dp),
                            border = if (isPlayingEp) null else androidx.compose.foundation.BorderStroke(1.dp, AnimeDarkSurfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Ep ${ep.displayIndex}",
                                    color = if (isPlayingEp) Color.White else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isPlayingEp) {
                                    Text(
                                        text = "Memutar",
                                        color = AnimeCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Episode Description Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AnimeDarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = currentEpisode?.title ?: "Episode ${currentEpisode?.displayIndex ?: 1}",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Server: ${uiState.selectedQuality?.name ?: "Direct Stream"} • Kualitas: ${uiState.selectedQuality?.quality ?: "720p"}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
