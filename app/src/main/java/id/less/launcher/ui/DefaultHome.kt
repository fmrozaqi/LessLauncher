package id.less.launcher.ui

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Whether pressing Home opens this launcher. */
fun Context.isDefaultHome(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
    }
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo?.packageName == packageName
}

/** Whether this launcher is the default home app, plus a way to ask the user to make it so. */
class DefaultHomeState(isDefault: Boolean, val request: () -> Unit) {
    var isDefault by mutableStateOf(isDefault)
        internal set
}

/**
 * [RequestPermissionsOnStart] asks once to become the default home app. After that the user can
 * still do it from Settings via [DefaultHomeState.request].
 */
@Composable
fun rememberDefaultHome(): DefaultHomeState {
    val context = LocalContext.current
    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val state = remember(context) {
        DefaultHomeState(context.isDefaultHome()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roles = context.getSystemService(RoleManager::class.java)
                if (roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    // Android stops showing this dialog after the user declines it twice,
                    // so fall back to the system's Home app settings.
                    try {
                        roleRequest.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                        return@DefaultHomeState
                    } catch (e: ActivityNotFoundException) {
                        // Some head units strip the role dialog.
                    }
                }
            }
            context.openHomeSettings()
        }
    }

    // The user may have changed the default home app in system Settings meanwhile.
    LifecycleResumeEffect(context) {
        state.isDefault = context.isDefaultHome()
        onPauseOrDispose { }
    }
    return state
}

private fun Context.openHomeSettings() {
    val intent = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
