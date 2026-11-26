package com.ronix.learning.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

val Bg = Color(0xFF09090B); val Panel = Color(0xFF121216); val Elev = Color(0xFF1A1A22)
val Purple = Color(0xFF8B5CF6); val Purple2 = Color(0xFFA855F7)
val TextP = Color(0xFFFAFAFA); val TextS = Color(0xFFA1A1AA); val Border = Color(0xFF27272A)

private val scheme = darkColorScheme(
    primary = Purple, secondary = Purple2, background = Bg, surface = Panel, surfaceVariant = Elev,
    onPrimary = Color.White, onBackground = TextP, onSurface = TextP, onSurfaceVariant = TextS,
    outline = Border, error = Color(0xFFF87171))

@Composable fun RonixTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, content = content)

@Composable fun RonixLogo(size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension / 64f
        drawRoundRect(Panel, cornerRadius = CornerRadius(16f * s, 16f * s))
        val st = Stroke(7f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(Path().apply { moveTo(18f * s, 18f * s); lineTo(32f * s, 32f * s); lineTo(18f * s, 46f * s) }, Purple, style = st)
        drawPath(Path().apply { moveTo(34f * s, 18f * s); lineTo(46f * s, 32f * s); lineTo(34f * s, 46f * s) }, Purple2.copy(alpha = .55f), style = st)
    }
}
