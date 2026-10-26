package com.fuwanime.ui.motion

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import kotlinx.coroutines.delay

/** Motion tokens: between snappy and cinematic, on Material 3's emphasized curves. */
object Motion {
    const val Short = 200
    const val Medium = 350
    const val Long = 420

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
}

/** Shared-element keys carry the row a title was opened from, so the same anime in two rows never clashes. */
fun sharedPosterKey(animeId: String, origin: String) = "poster-$animeId-$origin"
fun sharedEpisodeKey(episodeId: String, origin: String) = "episode-$episodeId-$origin"

private const val STAGGER_STEP_MS = 35
private const val STAGGER_MAX_STEPS = 8
private const val STAGGER_ITEMS = 12

fun staggerDelay(index: Int): Int = index.coerceIn(0, STAGGER_MAX_STEPS) * STAGGER_STEP_MS

/** The system "remove animations" setting sets the animator scale to 0. */
fun isReduceMotion(animatorScale: Float): Boolean = animatorScale == 0f

/** Remembers which list items already played their entrance, so scrolling back never replays it. */
class StaggerState {
    private val played = HashSet<Any>()

    /** True the first time an item within the first screenful shows up. */
    fun shouldAnimate(key: Any, index: Int): Boolean = index < STAGGER_ITEMS && played.add(key)
}

@Composable
fun rememberStaggerState(): StaggerState = remember { StaggerState() }

@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        isReduceMotion(Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** The enclosing navigation destination's enter/exit scope. */
val LocalNavScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

val LocalReduceMotion = staticCompositionLocalOf { false }

@OptIn(ExperimentalSharedTransitionApi::class)
private val SharedBounds = BoundsTransform { _, _ -> tween(Motion.Long, easing = Motion.Emphasized) }

/**
 * Makes this element fly between screens that use the same [key]. A no-op without a key, outside the
 * navigation scopes (previews, PiP) or when animations are off.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedKey(key: String?): Modifier = composed {
    val shared = LocalSharedScope.current
    val nav = LocalNavScope.current
    if (key == null || shared == null || nav == null || LocalReduceMotion.current) return@composed Modifier
    with(shared) {
        Modifier.sharedElement(rememberSharedContentState(key), nav, boundsTransform = SharedBounds)
    }
}

/** Destination content that rises into place after a shared element starts flying. */
fun Modifier.navEnter(delayMs: Int = 120): Modifier = composed {
    val nav = LocalNavScope.current
    if (nav == null || LocalReduceMotion.current) return@composed Modifier
    val rise = with(LocalDensity.current) { 24.dp.roundToPx() }
    with(nav) {
        Modifier.animateEnterExit(
            enter = fadeIn(tween(Motion.Medium, delayMs, Motion.EmphasizedDecelerate)) +
                slideInVertically(tween(Motion.Medium, delayMs, Motion.EmphasizedDecelerate)) { rise },
            exit = fadeOut(tween(Motion.Short, easing = Motion.EmphasizedAccelerate)),
        )
    }
}

/** First-appearance fade + rise for list items, staggered by [index] and played once per [state]. */
fun Modifier.staggerIn(state: StaggerState, key: Any, index: Int): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val animate = remember(key) { !reduce && state.shouldAnimate(key, index) }
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
