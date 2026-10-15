package com.fuwanime.ui.player

import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer

/** Bridges Picture-in-Picture between the activity and the player composable. */
class PipController(private val activity: ComponentActivity) {

    private val _isInPip = mutableStateOf(false)
    val isInPip: State<Boolean> = _isInPip

    val isSupported: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    private var autoEnter = false
    private var aspect = Rational(16, 9)

    private val listener = Consumer<PictureInPictureModeChangedInfo> { _isInPip.value = it.isInPictureInPictureMode }

    init {
        activity.addOnPictureInPictureModeChangedListener(listener)
    }

    /** Whether leaving the app (home gesture) should shrink the video into a floating window. */
    fun setAutoEnter(enabled: Boolean, width: Int = 16, height: Int = 9) {
        autoEnter = enabled && isSupported
        if (width > 0 && height > 0) {
            // Android rejects ratios outside roughly 1:2.39 .. 2.39:1.
            val r = width.toFloat() / height
            aspect = if (r in 0.42f..2.39f) Rational(width, height) else Rational(16, 9)
        }
        if (isSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { activity.setPictureInPictureParams(buildParams()) }
        }
    }

    fun enter(): Boolean {
        if (!isSupported || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return runCatching { activity.enterPictureInPictureMode(buildParams()) }.getOrDefault(false)
    }

    /** Pre-Android 12 has no auto-enter flag, so the activity forwards onUserLeaveHint here. */
    fun onUserLeaveHint() {
        if (autoEnter && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) enter()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildParams(): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder().setAspectRatio(aspect)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }
}

val LocalPipController = staticCompositionLocalOf<PipController?> { null }
