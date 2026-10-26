package id.less.launcher.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.less.launcher.R
import kotlinx.coroutines.delay
import java.util.Date

private val CardShape = RoundedCornerShape(24.dp)

/** The space the cards were designed for (a 1024x600 dp screen); anything smaller shrinks them. */
private val DesignWidth = 880.dp
private val DesignHeight = 512.dp

@Composable
fun ColumnScope.HomePage(musicApp: LauncherApp?) {
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        // Scale every dp and sp together so the clock and the player keep their proportions
        // instead of being cut off on small or low-resolution screens.
        val scale = minOf(1f, maxWidth / DesignWidth, maxHeight / DesignHeight)
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density * scale, density.fontScale)) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ClockCard()
                MusicCard(musicApp)
            }
        }
    }
}

@Composable
private fun RowScope.Card(
    modifier: Modifier = Modifier,
    verticalSpacing: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .neumorphicShadow(CardShape)
            .clip(CardShape)
            .background(LauncherColors.Surface)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun RowScope.ClockCard() {
    val context = LocalContext.current
    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            // Wake up right after the next minute starts.
            delay(60_000 - System.currentTimeMillis() % 60_000)
        }
    }
    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"

    Card(verticalSpacing = 6) {
        Text(
            text = DateFormat.format(timePattern, now).toString(),
            style = interStyle(124.sp, FontWeight.Light, LauncherColors.TextPrimary, letterSpacing = (-3.72).sp),
            maxLines = 1,
        )
        Text(
            text = DateFormat.format("EEEE", now).toString(),
            style = interStyle(22.sp, FontWeight.Medium, LauncherColors.TextPrimary),
        )
        Text(
            text = DateFormat.format("d MMMM yyyy", now).toString(),
            style = interStyle(18.sp, FontWeight.Normal, LauncherColors.TextSecondary),
        )
    }
}

/** What is playing in any media app, with working controls. Tapping it opens the player. */
@Composable
private fun RowScope.MusicCard(musicApp: LauncherApp?) {
    val context = LocalContext.current
    val nowPlaying = rememberNowPlaying(preferredPackage = musicApp?.packageName)
    val session = nowPlaying as? NowPlaying.Session
    val sourceLabel = remember(session?.packageName, musicApp) {
        session?.packageName?.let { context.appLabel(it) } ?: musicApp?.label ?: "Music"
    }
    val openPlayer = {
        if (session != null) context.openLauncherApp(LauncherApp(sourceLabel, packageName = session.packageName))
        else context.openLauncherApp(musicApp ?: LauncherApp("Music"))
    }

    Card(
        modifier = Modifier.clickable(onClick = openPlayer),
        verticalSpacing = 18,
    ) {
        SourceChip(sourceLabel)
        Disc(art = session?.art)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val (title, subtitle) = when (nowPlaying) {
                is NowPlaying.Session -> nowPlaying.title to nowPlaying.artist
                NowPlaying.NoAccess -> "Not connected" to "Allow access in Settings to show what's playing"
                NowPlaying.Idle -> "Not playing" to (musicApp?.let { "Tap to open ${it.label}" } ?: "No music app found")
            }
            Text(title, style = interStyle(22.sp, FontWeight.SemiBold, LauncherColors.TextPrimary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = interStyle(15.sp, FontWeight.Normal, LauncherColors.TextSecondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (session != null) {
            SessionProgress(session)
        } else {
            TrackProgress(elapsed = "0:00", total = "0:00", fraction = 0f)
        }
        Controls(
            playing = session?.isPlaying == true,
            onPrevious = { session?.previous() ?: openPlayer() },
            onPlayPause = {
                when (nowPlaying) {
                    is NowPlaying.Session -> nowPlaying.playPause()
                    NowPlaying.NoAccess -> context.openNotificationAccessSettings()
                    NowPlaying.Idle -> openPlayer()
                }
            },
            onNext = { session?.next() ?: openPlayer() },
        )
    }
}

/** The progress bar, moving every half second while the track plays. */
@Composable
private fun SessionProgress(session: NowPlaying.Session) {
    val position by produceState(session.positionMs(), session) {
        while (session.isPlaying) {
            value = session.positionMs()
            delay(500)
        }
        value = session.positionMs()
    }
    val duration = session.durationMs
    TrackProgress(
        elapsed = formatTime(position),
        total = if (duration > 0) formatTime(duration) else "--:--",
        fraction = if (duration > 0) position.toFloat() / duration else 0f,
    )
}

private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val hours = seconds / 3600
    return if (hours > 0) "%d:%02d:%02d".format(hours, seconds / 60 % 60, seconds % 60)
    else "%d:%02d".format(seconds / 60, seconds % 60)
}

private fun Context.appLabel(packageName: String): String? = try {
    packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
} catch (e: PackageManager.NameNotFoundException) {
    null
}

@Composable
private fun SourceChip(label: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(LauncherColors.SurfaceRaised)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_music_small), contentDescription = null)
        Text(label, style = interStyle(13.sp, FontWeight.Medium, LauncherColors.TextSecondary), maxLines = 1)
    }
}

@Composable
private fun Disc(art: Bitmap?) {
    Box(
        modifier = Modifier
            .size(176.dp)
            .neumorphicShadow(CircleShape)
            .clip(CircleShape)
            .background(LauncherColors.SurfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        0f to LauncherColors.Accent,
                        0.71429f to LauncherColors.AlbumGradientEnd,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (art != null) {
                val image = remember(art) { art.asImageBitmap() }
                Image(image, contentDescription = "Album art", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Image(painterResource(R.drawable.ic_note), contentDescription = null)
            }
        }
    }
}

@Composable
private fun TrackProgress(elapsed: String, total: String, fraction: Float) {
    val timeStyle = interStyle(12.sp, FontWeight.Normal, LauncherColors.TextSecondary)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(elapsed, style = timeStyle)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LauncherColors.Divider),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LauncherColors.Accent),
            )
        }
        Text(total, style = timeStyle)
    }
}

@Composable
private fun Controls(playing: Boolean, onPrevious: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(R.drawable.ic_prev),
            contentDescription = "Previous",
            modifier = Modifier.clip(CircleShape).clickable(onClick = onPrevious),
        )
        Box(
            modifier = Modifier
                .size(64.dp)
                .dropShadow(
                    CircleShape,
                    Shadow(radius = 20.dp, color = LauncherColors.Accent.copy(alpha = 0.4f), offset = DpOffset(0.dp, 6.dp)),
                )
                .clip(CircleShape)
                .background(LauncherColors.Accent)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = if (playing) "Pause" else "Play",
            )
        }
        Image(
            painterResource(R.drawable.ic_next),
            contentDescription = "Next",
            modifier = Modifier.clip(CircleShape).clickable(onClick = onNext),
        )
    }
}
