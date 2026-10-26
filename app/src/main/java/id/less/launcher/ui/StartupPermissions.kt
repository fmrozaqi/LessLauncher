package id.less.launcher.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Something the launcher needs the user to grant, asked for once at most. */
private class StartupRequest(val key: String, val granted: () -> Boolean, val request: () -> Unit)

/**
 * On start, asks for everything the launcher needs, one after another: to become the default
 * home app, then notification access for the Home music card. Each is asked once; whatever the
 * user declines can still be granted from Settings.
 */
@Composable
fun RequestPermissionsOnStart(defaultHome: DefaultHomeState) {
    if (LocalInspectionMode.current) return
    val context = LocalContext.current
    val requests = remember(context, defaultHome) {
        listOf(
            StartupRequest("asked_default_home", granted = { context.isDefaultHome() }, request = defaultHome.request),
            StartupRequest(
                "asked_notification_access",
                granted = { context.hasNotificationAccess() },
                request = { context.openNotificationAccessSettings() },
            ),
        )
    }

    // Every request leaves the launcher for a dialog or a Settings screen. Coming back resumes
    // the launcher, which moves on to the next request.
    LifecycleResumeEffect(context) {
        val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)
        requests.firstOrNull { !prefs.getBoolean(it.key, false) && !it.granted() }?.let {
            prefs.edit { putBoolean(it.key, true) }
            it.request()
        }
        onPauseOrDispose { }
    }
}
