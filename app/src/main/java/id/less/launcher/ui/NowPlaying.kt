package id.less.launcher.ui

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Does nothing by itself. While the user grants it notification access, Android lets the
 * launcher read and control other apps' media sessions.
 */
class MediaListenerService : NotificationListenerService()

sealed interface NowPlaying {
    /** The user has not granted notification access yet. */
    data object NoAccess : NowPlaying

    /** No app has a media session. */
    data object Idle : NowPlaying

    class Session(
        val packageName: String,
        val title: String,
        val artist: String,
        val art: Bitmap?,
        val durationMs: Long,
        private val state: PlaybackState?,
        private val controls: MediaController.TransportControls,
    ) : NowPlaying {
        val isPlaying get() = state?.state == PlaybackState.STATE_PLAYING ||
            state?.state == PlaybackState.STATE_BUFFERING

        /** The playback position right now, extrapolated from the session's last report. */
        fun positionMs(): Long {
            val state = state ?: return 0
            if (state.position < 0) return 0
            val position = if (state.state == PlaybackState.STATE_PLAYING) {
                state.position + ((SystemClock.elapsedRealtime() - state.lastPositionUpdateTime) * state.playbackSpeed).toLong()
            } else {
                state.position
            }
            return if (durationMs > 0) position.coerceIn(0, durationMs) else position
        }

        fun playPause() = if (isPlaying) controls.pause() else controls.play()
        fun previous() = controls.skipToPrevious()
        fun next() = controls.skipToNext()
    }
}

fun Context.hasNotificationAccess(): Boolean =
    packageName in NotificationManagerCompat.getEnabledListenerPackages(this)

/** Opens the notification access switch for this launcher, or the list of all listeners. */
fun Context.openNotificationAccessSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(this, MediaListenerService::class.java).flattenToString(),
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(detail)
            return
        } catch (e: ActivityNotFoundException) {
            // Some head units strip the detail screen.
        }
    }
    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private const val MaxConnectAttempts = 10

/**
 * The media that is playing. A playing session wins; otherwise [preferredPackage]'s session
 * (the Music slot's app), otherwise the most recently active one.
 */
@Composable
fun rememberNowPlaying(preferredPackage: String?): NowPlaying {
    if (LocalInspectionMode.current) return NowPlaying.Idle
    val context = LocalContext.current

    // Notification access is granted in system Settings, so check again on every return.
    var hasAccess by remember { mutableStateOf(context.hasNotificationAccess()) }
    LifecycleResumeEffect(context) {
        hasAccess = context.hasNotificationAccess()
        onPauseOrDispose { }
    }
    if (!hasAccess) return NowPlaying.NoAccess

    var controllers by remember { mutableStateOf(emptyList<MediaController>()) }
    // Bumped on every metadata or playback change, since MediaController is not observable.
    var changes by remember { mutableIntStateOf(0) }

    // Bumped to connect again after the system refused the media sessions.
    var attempt by remember { mutableIntStateOf(0) }

    DisposableEffect(context, attempt) {
        val manager = context.getSystemService(MediaSessionManager::class.java)
        val listener = ComponentName(context, MediaListenerService::class.java)
        val handler = Handler(Looper.getMainLooper())
        val onSessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { controllers = it.orEmpty() }
        try {
            manager.addOnActiveSessionsChangedListener(onSessionsChanged, listener, handler)
            controllers = manager.getActiveSessions(listener)
        } catch (e: SecurityException) {
            // Right after the user grants access, the media service can still refuse for a
            // moment, so try again shortly. If it keeps refusing, access was really revoked.
            if (attempt < MaxConnectAttempts) handler.postDelayed({ attempt++ }, 500)
            else hasAccess = false
        }
        onDispose {
            handler.removeCallbacksAndMessages(null)
            manager.removeOnActiveSessionsChangedListener(onSessionsChanged)
        }
    }

    DisposableEffect(controllers) {
        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) { changes++ }
            override fun onPlaybackStateChanged(state: PlaybackState?) { changes++ }
            override fun onSessionDestroyed() { changes++ }
        }
        val handler = Handler(Looper.getMainLooper())
        controllers.forEach { it.registerCallback(callback, handler) }
        onDispose { controllers.forEach { it.unregisterCallback(callback) } }
    }

    return remember(controllers, changes, preferredPackage) { controllers.pick(preferredPackage) }
}

private fun List<MediaController>.pick(preferredPackage: String?): NowPlaying {
    val controller = firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        ?: firstOrNull { it.packageName == preferredPackage }
        ?: firstOrNull()
        ?: return NowPlaying.Idle

    val metadata = controller.metadata
    return NowPlaying.Session(
        packageName = controller.packageName,
        title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: "Unknown title",
        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: "",
        art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
        durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0,
        state = controller.playbackState,
        controls = controller.transportControls,
    )
}
