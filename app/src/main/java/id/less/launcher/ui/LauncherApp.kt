package id.less.launcher.ui

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * An app the launcher can open.
 *
 * [packageName] is null while the app is a dummy. To connect it to a real app later,
 * set its package name (e.g. "com.android.settings") and it will open that app on tap.
 * [component] pins the exact activity, for apps found on the device.
 */
data class LauncherApp(
    val label: String,
    val packageName: String? = null,
    val component: ComponentName? = null,
)

fun Context.openLauncherApp(app: LauncherApp) {
    val intent = when {
        app.component != null -> Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        app.packageName != null -> packageManager.getLaunchIntentForPackage(app.packageName)
        else -> null
    }
    if (intent == null) {
        if (app.packageName == null) {
            showComingSoon(app.label)
        } else {
            Toast.makeText(this, "${app.label} is not installed", Toast.LENGTH_SHORT).show()
        }
        return
    }
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "${app.label} is not installed", Toast.LENGTH_SHORT).show()
    }
}

fun Context.showComingSoon(label: String) {
    Toast.makeText(this, "$label: coming soon", Toast.LENGTH_SHORT).show()
}
