package id.less.launcher.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.less.launcher.R

private val ListShape = RoundedCornerShape(20.dp)
private val RowHeight = 64.dp
private val ListVerticalPadding = 4.dp

@Composable
fun ColumnScope.AllAppsPage(apps: List<LauncherApp>, onBack: () -> Unit) {
    val context = LocalContext.current

    PageHeader("All Apps", onBack = onBack, trailing = "${apps.size} apps")
    ListCard(itemCount = apps.size) {
        items(apps, key = { it.component?.flattenToString() ?: it.label }) { app ->
            ListRow(app.label, onClick = { context.openLauncherApp(app) })
        }
    }
}

@Composable
fun PageHeader(title: String, onBack: () -> Unit, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onBack),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_back), contentDescription = "Back")
            Text(title, style = interStyle(24.sp, FontWeight.SemiBold, LauncherColors.TextPrimary))
        }
        if (trailing != null) {
            Text(trailing, style = interStyle(14.sp, FontWeight.Normal, LauncherColors.TextSecondary))
        }
    }
}

/** A scrolling card of fixed-height [ListRow]s that fills the rest of the page. */
@Composable
fun ColumnScope.ListCard(itemCount: Int, content: LazyListScope.() -> Unit) {
    val listState = rememberLazyListState()
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .neumorphicShadow(ListShape)
            .clip(ListShape)
            .background(LauncherColors.Surface),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = ListVerticalPadding),
            content = content,
        )
        ScrollIndicator(
            state = listState,
            itemCount = itemCount,
            viewportHeight = maxHeight,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
fun ListRow(
    label: String,
    onClick: () -> Unit,
    @DrawableRes icon: Int? = null,
    detail: String? = null,
    checked: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clickable(onClick = onClick)
            .drawBehind {
                val y = size.height - 0.5.dp.toPx()
                drawLine(LauncherColors.Divider, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Image(painterResource(icon), contentDescription = null)
        Text(
            label,
            style = interStyle(18.sp, FontWeight.Normal, LauncherColors.TextPrimary),
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (detail != null) {
            Text(detail, style = interStyle(15.sp, FontWeight.Normal, LauncherColors.TextSecondary), maxLines = 1)
        }
        if (checked) Image(painterResource(R.drawable.ic_check), contentDescription = "Selected")
    }
}

/** The thin scrollbar on the right edge of the list. Rows have a fixed height, so position is exact. */
@Composable
private fun ScrollIndicator(
    state: LazyListState,
    itemCount: Int,
    viewportHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val trackInset = 14.dp
    val contentHeight = RowHeight * itemCount + ListVerticalPadding * 2
    if (contentHeight <= viewportHeight) return

    val trackHeight = viewportHeight - trackInset * 2
    val thumbHeight = maxOf(trackHeight * (viewportHeight / contentHeight), 32.dp)
    val density = LocalDensity.current
    val scrolled = with(density) {
        RowHeight * state.firstVisibleItemIndex + state.firstVisibleItemScrollOffset.toDp()
    }
    val maxScroll = contentHeight - viewportHeight
    val thumbOffset = (trackHeight - thumbHeight) * (scrolled / maxScroll).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .padding(top = trackInset, end = 8.dp)
            .offset(y = thumbOffset)
            .width(4.dp)
            .height(thumbHeight)
            .clip(CircleShape)
            .background(LauncherColors.TextSecondary.copy(alpha = 0.5f)),
    )
}
