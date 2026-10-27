package com.fuwanime.ui.motion

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.delay

/** Motion tokens: between snappy and cinematic, on Material 3's emphasized curves. */
object Motion {
    const val Short = 200
    const val Medium = 350

    /** Page slide/fade when opening or closing detail and player. */
    const val Page = 250
    const val PageExit = 200

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

/** Opens an anime's detail page; [from] names the row it was tapped in, for the shared poster key. */
typealias OpenAnime = (anime: com.fuwanime.data.Anime, from: String) -> Unit

/** Shared-element keys carry the row a title was opened from, so the same anime in two rows never clashes. */
fun sharedPosterKey(animeId: String, origin: String) = "poster-$animeId-$origin"
fun sharedEpisodeKey(episodeId: String, origin: String) = "episode-$episodeId-$origin"

/** The detail header backdrop, which flies into the player when "Tonton"/"Lanjutkan" is pressed. */
fun sharedBackdropKey(animeId: String, origin: String) = "backdrop-$animeId-$origin"

private const val STAGGER_STEP_MS = 35
private const val STAGGER_MAX_STEPS = 8
private const val STAGGER_ITEMS = 12

/** Only items composed this soon after a list first shows count as "first screenful". */
private const val STAGGER_WINDOW_MS = 600L

fun staggerDelay(index: Int): Int = index.coerceIn(0, STAGGER_MAX_STEPS) * STAGGER_STEP_MS

/**
 * Whether a list's header is still on screen. A shared element only links to it then: flying to or from a header
 * scrolled off the top looks like the image chasing the list.
 */
fun headerStillShown(firstVisibleIndex: Int, firstVisibleOffsetPx: Int, maxOffsetPx: Int): Boolean =
    firstVisibleIndex == 0 && firstVisibleOffsetPx < maxOffsetPx

/** The system "remove animations" setting sets the animator scale to 0. */
fun isReduceMotion(animatorScale: Float): Boolean = animatorScale == 0f

/**
 * Remembers which list items already played their entrance, so scrolling back or returning from another
 * screen never replays it. Saveable, so it survives lazy-item disposal and back navigation.
 */
class StaggerState private constructor(private val played: HashSet<String>) {
    constructor() : this(HashSet())

    /** Start of this list's first appearance; items composed long after it come from scrolling. */
    val createdAtMs: Long = System.currentTimeMillis()

    /** True the first time an early item shows up within the first-appearance window. */
    fun shouldAnimate(key: String, index: Int, elapsedMs: Long): Boolean =
        index < STAGGER_ITEMS && elapsedMs < STAGGER_WINDOW_MS && played.add(key)

    fun save(): List<String> = played.toList()

    companion object {
        fun restore(saved: List<String>) = StaggerState(saved.toHashSet())

        val Saver: Saver<StaggerState, ArrayList<String>> = Saver(save = { ArrayList(it.save()) }, restore = { restore(it) })
    }
}

@Composable
fun rememberStaggerState(): StaggerState = rememberSaveable(saver = StaggerState.Saver) { StaggerState() }

@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        isReduceMotion(Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
    }
}

/** The enclosing navigation destination's enter/exit scope. */
val LocalNavScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

val LocalReduceMotion = staticCompositionLocalOf { false }

/** True while this destination is animating in (for dropping shared-element links the user scrolls away from). */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun rememberIsEntering(): Boolean {
    val transition = LocalNavScope.current?.transition ?: return false
    return transition.isRunning && transition.targetState == EnterExitState.Visible
}

/** True while this destination is leaving (closing, or being covered by the next page). */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun rememberIsExiting(): Boolean = LocalNavScope.current?.transition?.targetState == EnterExitState.PostExit

/**
 * Lets touches fall through to the page beneath while this one is leaving: it still draws (and its shared elements
 * still fly), but it reports no size, so hit testing skips it. Without this the fading page swallowed every tap
 * until its poster landed.
 */
fun Modifier.passTouchesThrough(enabled: Boolean): Modifier = if (!enabled) this else layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(0, 0) { placeable.place(0, 0) }
}

/** First-appearance fade + rise for list items, staggered by [index] and played once per [state]. */
fun Modifier.staggerIn(state: StaggerState, key: String, index: Int): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val animate = remember(key) {
        !reduce && state.shouldAnimate(key, index, System.currentTimeMillis() - state.createdAtMs)
    }
    val progress = remember(key) { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(key) {
        if (animate) {
            delay(staggerDelay(index).toLong())
            progress.animateTo(1f, tween(Motion.Medium, easing = Motion.EmphasizedDecelerate))
        }
    }
    val rise = with(LocalDensity.current) { 16.dp.toPx() }
    Modifier.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * rise
    }
}

/** Shrinks slightly while pressed; pass the same [interactionSource] to the clickable. */
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(Motion.Short, easing = Motion.Emphasized), label = "pressScale")
    Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
