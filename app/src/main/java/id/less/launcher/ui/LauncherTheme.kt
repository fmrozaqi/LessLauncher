package id.less.launcher.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import id.less.launcher.R

/** Color tokens from the Figma file variables. */
object LauncherColors {
    val Bg = Color(0xFF14161B)
    val Sidebar = Color(0xFF101216)
    val Surface = Color(0xFF1C1F26)
    val SurfaceRaised = Color(0xFF252933)
    val Divider = Color(0xFF2A2E38)
    val TextPrimary = Color(0xFFE8EAF0)
    val TextSecondary = Color(0xFF8A90A0)
    val Accent = Color(0xFF3D6BFF)
    val AlbumGradientEnd = Color(0xFF8F4DF2)
}

val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

fun interStyle(
    size: TextUnit,
    weight: FontWeight,
    color: Color,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size,
    // Inter's "normal" line height in Figma.
    lineHeight = 1.21.em,
    letterSpacing = letterSpacing,
    color = color,
)

/** The soft light + dark shadow pair used on cards in the design. */
fun Modifier.neumorphicShadow(shape: Shape): Modifier = this
    .dropShadow(
        shape,
        Shadow(radius = 20.dp, color = Color.Black.copy(alpha = 0.45f), offset = DpOffset(8.dp, 8.dp)),
    )
    .dropShadow(
        shape,
        Shadow(radius = 16.dp, color = Color.White.copy(alpha = 0.04f), offset = DpOffset((-6).dp, (-6).dp)),
    )
