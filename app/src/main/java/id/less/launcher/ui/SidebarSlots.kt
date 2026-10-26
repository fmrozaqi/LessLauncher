package id.less.launcher.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import id.less.launcher.R

/**
 * A sidebar shortcut. It opens the app the user picked in Settings, or else the first
 * installed app that matches [knownPackages] (in order) or one of [keywords] in its name.
 */
enum class SidebarSlot(
    val label: String,
    @DrawableRes val icon: Int,
    private val knownPackages: List<String>,
    private val keywords: List<String>,
    /** An app category whose apps also count as a match, e.g. [Intent.CATEGORY_APP_MUSIC]. */
    private val category: String? = null,
) {
    Radio(
        label = "Radio",
        icon = R.drawable.ic_radio,
        knownPackages = listOf(
            "com.android.fmradio",
            "com.caf.fmradio",
            "com.android.radio",
            "com.syu.radio",
            "com.microntek.radio",
        ),
        keywords = listOf("radio", "fm"),
    ),
    Music(
        label = "Music",
        icon = R.drawable.ic_music,
        knownPackages = listOf(
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.apple.android.music",
            "deezer.android.app",
            "com.maxmpz.audioplayer",
            "com.android.music",
            "com.sec.android.app.music",
            "com.miui.player",
        ),
        keywords = listOf("music", "musik"),
        category = Intent.CATEGORY_APP_MUSIC,
    ),
    AndroidAuto(
        label = "Android Auto",
        icon = R.drawable.ic_car,
        knownPackages = listOf("com.google.android.projection.gearhead", "com.zjinnova.zlink"),
        keywords = listOf("android auto", "autokit", "zlink", "carlink"),
    ),
    CarPlay(
        label = "CarPlay",
        icon = R.drawable.ic_carplay,
        knownPackages = listOf("com.zjinnova.zlink"),
        keywords = listOf("carplay", "car play", "autokit", "zlink", "carlink"),
    ),
    ;

    /** The installed app that best fits this slot, or null when nothing fits. */
    fun detect(context: Context, apps: List<LauncherApp>): LauncherApp? {
        knownPackages.firstNotNullOfOrNull { pkg -> apps.firstOrNull { it.packageName == pkg } }?.let { return it }
        if (category != null) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            val packages = context.packageManager.queryIntentActivities(intent, 0)
                .map { it.activityInfo.packageName }
            apps.firstOrNull { it.packageName in packages }?.let { return it }
        }
        return apps.firstOrNull { app ->
            // Match whole words, so "fm" does not hit "Filmora".
            val words = " " + app.label.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ") + " "
            keywords.any { " $it " in words }
        }
    }
}

/** What a slot opens: the app (null = none found) and whether it was picked automatically. */
data class SlotBinding(val app: LauncherApp?, val automatic: Boolean)

/** The apps the user picked for each slot in Settings. A slot with no pick is automatic. */
class SlotChoices(context: Context) {
    private val prefs = context.getSharedPreferences("sidebar_slots", Context.MODE_PRIVATE)
    private val choices = mutableStateMapOf<SidebarSlot, ComponentName>().apply {
        SidebarSlot.entries.forEach { slot ->
            prefs.getString(slot.name, null)?.let(ComponentName::unflattenFromString)?.let { put(slot, it) }
        }
    }

    /** Pins [slot] to [component], or makes it automatic again when [component] is null. */
    fun choose(slot: SidebarSlot, component: ComponentName?) {
        if (component == null) choices.remove(slot) else choices[slot] = component
        prefs.edit { putString(slot.name, component?.flattenToString()) }
    }

    /** A picked app that was uninstalled falls back to the [detected] one. */
    fun resolve(apps: List<LauncherApp>, detected: Map<SidebarSlot, LauncherApp?>): Map<SidebarSlot, SlotBinding> =
        SidebarSlot.entries.associateWith { slot ->
            val chosen = choices[slot]?.let { component -> apps.firstOrNull { it.component == component } }
            if (chosen != null) SlotBinding(chosen, automatic = false)
            else SlotBinding(detected[slot], automatic = true)
        }
}

@Composable
fun rememberSlotChoices(): SlotChoices {
    val context = LocalContext.current
    return remember(context) { SlotChoices(context) }
}

/** What every slot opens right now. Updates when apps change or the user picks a different one. */
@Composable
fun rememberSlotBindings(apps: List<LauncherApp>, choices: SlotChoices): Map<SidebarSlot, SlotBinding> {
    val context = LocalContext.current
    val detected = remember(apps) { SidebarSlot.entries.associateWith { it.detect(context, apps) } }
    return choices.resolve(apps, detected)
}

fun Context.openSlot(slot: SidebarSlot, binding: SlotBinding?) {
    // With nothing bound it stays a dummy, which shows "coming soon".
    openLauncherApp(binding?.app ?: LauncherApp(slot.label))
}
