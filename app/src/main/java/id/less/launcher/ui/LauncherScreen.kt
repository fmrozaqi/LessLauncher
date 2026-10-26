package id.less.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.less.launcher.R

enum class LauncherPage { Home, AllApps, Settings }

/**
 * Root of the launcher: sidebar on the left, status bar + current page on the right.
 *
 * [homeRequests] increases every time the Home button is pressed while the launcher is
 * already open, which brings it back to the Home page.
 */
@Composable
fun LauncherScreen(homeRequests: Int = 0) {
    var page by rememberSaveable { mutableStateOf(LauncherPage.Home) }
    LaunchedEffect(homeRequests) { page = LauncherPage.Home }
    BackHandler(enabled = page != LauncherPage.Home) { page = LauncherPage.Home }

    val apps = rememberInstalledApps()
    val choices = rememberSlotChoices()
    val bindings = rememberSlotBindings(apps, choices)
    val defaultHome = rememberDefaultHome()
    RequestPermissionsOnStart(defaultHome)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(LauncherColors.Bg),
    ) {
        Sidebar(
            bindings = bindings,
            appsActive = page == LauncherPage.AllApps,
            onAppsClick = {
                page = if (page == LauncherPage.AllApps) LauncherPage.Home else LauncherPage.AllApps
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusBar(
                settingsActive = page == LauncherPage.Settings,
                onSettingsClick = {
                    page = if (page == LauncherPage.Settings) LauncherPage.Home else LauncherPage.Settings
                },
            )
            when (page) {
                LauncherPage.Home -> HomePage(musicApp = bindings[SidebarSlot.Music]?.app)
                LauncherPage.AllApps -> AllAppsPage(apps, onBack = { page = LauncherPage.Home })
                LauncherPage.Settings -> SettingsPage(apps, choices, bindings, defaultHome, onBack = { page = LauncherPage.Home })
            }
        }
    }
}

@Composable
private fun Sidebar(
    bindings: Map<SidebarSlot, SlotBinding>,
    appsActive: Boolean,
    onAppsClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(96.dp)
            .fillMaxHeight()
            .background(LauncherColors.Sidebar)
            .drawBehind {
                val x = size.width - 0.5.dp.toPx()
                drawLine(LauncherColors.Divider, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            }
            .padding(horizontal = 10.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SidebarSlot.entries.forEach { slot ->
            SidebarItem(slot.icon, slot.label) { context.openSlot(slot, bindings[slot]) }
        }
        SidebarItem(
            icon = if (appsActive) R.drawable.ic_apps_active else R.drawable.ic_apps,
            label = "Apps",
            active = appsActive,
            onClick = onAppsClick,
        )
    }
}

@Composable
private fun ColumnScope.SidebarItem(
    @DrawableRes icon: Int,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) LauncherColors.SurfaceRaised else LauncherColors.Sidebar)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(icon), contentDescription = null)
        Text(
            text = label,
            style = interStyle(
                size = 12.sp,
                weight = FontWeight.Medium,
                color = if (active) LauncherColors.TextPrimary else LauncherColors.TextSecondary,
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun StatusBar(settingsActive: Boolean, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_bluetooth), contentDescription = "Bluetooth")
        Image(painterResource(R.drawable.ic_wifi), contentDescription = "Wi-Fi")
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (settingsActive) LauncherColors.SurfaceRaised else LauncherColors.Bg)
                .clickable(onClick = onSettingsClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_settings), contentDescription = "Settings")
        }
    }
}

@Preview(widthDp = 1024, heightDp = 600)
@Composable
private fun LauncherScreenPreview() {
    LauncherScreen()
}

@Preview(widthDp = 800, heightDp = 480)
@Composable
private fun LauncherScreenSmallPreview() {
    LauncherScreen()
}
