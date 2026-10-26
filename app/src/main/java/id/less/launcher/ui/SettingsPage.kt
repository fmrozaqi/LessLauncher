package id.less.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import id.less.launcher.R

/** Launcher settings: which app each sidebar shortcut opens, and media access for the Music card. */
@Composable
fun ColumnScope.SettingsPage(
    apps: List<LauncherApp>,
    choices: SlotChoices,
    bindings: Map<SidebarSlot, SlotBinding>,
    defaultHome: DefaultHomeState,
    onBack: () -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf<SidebarSlot?>(null) }
    val slot = editing
    if (slot == null) {
        SlotList(bindings, defaultHome, onSlotClick = { editing = it }, onBack = onBack)
    } else {
        BackHandler { editing = null }
        AppPicker(slot, apps, choices, bindings[slot], onDone = { editing = null })
    }
}

@Composable
private fun ColumnScope.SlotList(
    bindings: Map<SidebarSlot, SlotBinding>,
    defaultHome: DefaultHomeState,
    onSlotClick: (SidebarSlot) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var hasAccess by remember { mutableStateOf(context.hasNotificationAccess()) }
    LifecycleResumeEffect(context) {
        hasAccess = context.hasNotificationAccess()
        onPauseOrDispose { }
    }

    PageHeader("Settings", onBack = onBack)
    ListCard(itemCount = SidebarSlot.entries.size + 2) {
        items(SidebarSlot.entries, key = { it.name }) { slot ->
            ListRow(
                label = slot.label,
                icon = slot.icon,
                detail = bindings[slot].describe(),
                onClick = { onSlotClick(slot) },
            )
        }
        item(key = "media-access") {
            ListRow(
                label = "Now playing on Home",
                icon = R.drawable.ic_music_small,
                detail = if (hasAccess) "Allowed" else "Tap to allow",
                onClick = { context.openNotificationAccessSettings() },
            )
        }
        item(key = "default-home") {
            ListRow(
                label = "Default home app",
                icon = R.drawable.ic_apps,
                detail = if (defaultHome.isDefault) "Yes" else "Tap to set",
                onClick = defaultHome.request,
            )
        }
    }
}

@Composable
private fun ColumnScope.AppPicker(
    slot: SidebarSlot,
    apps: List<LauncherApp>,
    choices: SlotChoices,
    binding: SlotBinding?,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val detected = remember(slot, apps) { slot.detect(context, apps) }
    val automatic = binding?.automatic != false

    PageHeader("${slot.label} app", onBack = onDone)
    ListCard(itemCount = apps.size + 1) {
        item(key = "automatic") {
            ListRow(
                label = "Automatic",
                detail = detected?.label ?: "No app found",
                checked = automatic,
                onClick = {
                    choices.choose(slot, null)
                    onDone()
                },
            )
        }
        items(apps, key = { it.component?.flattenToString() ?: it.label }) { app ->
            ListRow(
                label = app.label,
                checked = !automatic && binding?.app == app,
                onClick = {
                    choices.choose(slot, app.component)
                    onDone()
                },
            )
        }
    }
}

private fun SlotBinding?.describe(): String = when {
    this == null || app == null -> "No app found"
    automatic -> "${app.label} · Auto"
    else -> app.label
}
