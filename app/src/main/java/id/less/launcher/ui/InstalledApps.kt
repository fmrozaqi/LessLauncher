package id.less.launcher.ui

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

/** Every app on the device that has a launcher icon, sorted by name. Excludes this launcher. */
fun Context.loadInstalledApps(): List<LauncherApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val collator = Collator.getInstance()
    return packageManager.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != packageName }
        .map { info ->
            LauncherApp(
                label = info.loadLabel(packageManager).toString(),
                packageName = info.activityInfo.packageName,
                component = ComponentName(info.activityInfo.packageName, info.activityInfo.name),
            )
        }
        .sortedWith(compareBy(collator) { it.label })
}

/** The installed apps, reloaded whenever an app is installed, updated or removed. */
@Composable
fun rememberInstalledApps(): List<LauncherApp> {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
    var packageChanges by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                packageChanges++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(packageChanges) {
        apps = withContext(Dispatchers.IO) { context.loadInstalledApps() }
    }
    return apps
}
