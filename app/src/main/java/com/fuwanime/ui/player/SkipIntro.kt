package com.fuwanime.ui.player

import com.fuwanime.data.OpMark

sealed interface SkipAction {
    val targetMs: Long

    /** Inside a marked opening: jump to its end. */
    data class SkipOp(override val targetMs: Long) : SkipAction

    /** No mark yet: a generic jump that clears most openings. */
    data class JumpAhead(override val targetMs: Long) : SkipAction
}

private const val JUMP_MS = 85_000L
private const val JUMP_FROM_MS = 5_000L
private const val JUMP_UNTIL_MS = 180_000L
private const val GUESSED_OP_MS = 90_000L

fun skipActionAt(positionMs: Long, durationMs: Long, mark: OpMark?): SkipAction? {
    if (mark != null && mark.isValid) {
        // Leave the last second alone: skipping it is pointless and the button would flash.
        return if (positionMs >= mark.startMs && positionMs < mark.endMs - 1_000) SkipAction.SkipOp(mark.endMs) else null
    }
    if (positionMs < JUMP_FROM_MS || positionMs >= JUMP_UNTIL_MS) return null
    val target = positionMs + JUMP_MS
    return if (durationMs <= 0 || target < durationMs) SkipAction.JumpAhead(target) else null
}

/** Sets one edge of the mark at [positionMs], keeping the other edge when it still makes sense. */
fun markOp(current: OpMark?, positionMs: Long, isStart: Boolean): OpMark = if (isStart) {
    OpMark(positionMs, current?.endMs?.takeIf { it > positionMs } ?: (positionMs + GUESSED_OP_MS))
} else {
    OpMark(current?.startMs?.takeIf { it < positionMs } ?: (positionMs - GUESSED_OP_MS).coerceAtLeast(0), positionMs)
}
