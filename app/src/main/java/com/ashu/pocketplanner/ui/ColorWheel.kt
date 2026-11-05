package com.ashu.pocketplanner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private fun hsvToArgb(h: Float, s: Float, v: Float): Long =
    android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)).toLong() and 0xFFFFFFFFL

private val QUICK = listOf(
    0xFF0B0A1AL, 0xFF2B2F6BL, 0xFF3A1C5CL, 0xFF5B1A3AL, 0xFF0F3B3AL, 0xFF14361FL,
    0xFF4A2A0FL, 0xFFF4E9DCL, 0xFFDCEBFFL, 0xFFFFD9E6L, 0xFFE3F7E8L, 0xFF000000L,
)

/**
 * Pick any colour, like the colour circle in Paint: the angle is the hue, distance from
 * the middle is how strong the colour is, and the slider sets how bright it is.
 */
@Composable
fun ColorWheel(argb: Long, onChange: (Long) -> Unit) {
    val p = LocalPalette.current
    val start = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(argb.toInt(), it) }
    }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    val emit by rememberUpdatedState(onChange)

    fun pickAt(pos: Offset, w: Float, h: Float) {
        val cx = w / 2f
        val cy = h / 2f
        val r = min(cx, cy)
        val dx = pos.x - cx
        val dy = pos.y - cy
        var deg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        if (deg < 0) deg += 360f
        hue = deg
        sat = (sqrt(dx * dx + dy * dy) / r).coerceIn(0f, 1f)
        emit(hsvToArgb(hue, sat, value))
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().widthIn(max = 280.dp), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .widthIn(max = 260.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(Unit) {
                        detectTapGestures { pickAt(it, size.width.toFloat(), size.height.toFloat()) }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            pickAt(change.position, size.width.toFloat(), size.height.toFloat())
                        }
                    },
            ) {
                val r = min(size.width, size.height) / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    Brush.sweepGradient(
                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                        center = c,
                    ),
                    radius = r, center = c,
                )
                drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), center = c, radius = r), radius = r, center = c)
                drawCircle(Color.Black.copy(alpha = 1f - value), radius = r, center = c)
                val rad = Math.toRadians(hue.toDouble())
                val m = Offset(c.x + (cos(rad) * sat * r).toFloat(), c.y + (sin(rad) * sat * r).toFloat())
                drawCircle(Color(hsvToArgb(hue, sat, value)), radius = 14.dp.toPx(), center = m)
                drawCircle(Color.White, radius = 14.dp.toPx(), center = m, style = Stroke(width = 3.dp.toPx()))
                drawCircle(Color.Black.copy(alpha = 0.4f), radius = 15.5.dp.toPx(), center = m, style = Stroke(width = 1.dp.toPx()))
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("☀️", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = value,
                onValueChange = {
                    value = it
                    emit(hsvToArgb(hue, sat, value))
                },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color(hsvToArgb(hue, sat, value)), activeTrackColor = p.accent, inactiveTrackColor = p.glassHi),
            )
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(hsvToArgb(hue, sat, value))).border(1.dp, p.border, CircleShape))
        }
        Text(
            String.format(Locale.US, "#%06X", hsvToArgb(hue, sat, value) and 0xFFFFFFL),
            style = MaterialTheme.typography.labelMedium,
            color = p.sub,
            fontWeight = FontWeight.Medium,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QUICK.forEach { q ->
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(q))
                        .border(1.dp, p.border, CircleShape)
                        .clickable {
                            val f = FloatArray(3)
                            android.graphics.Color.colorToHSV(q.toInt(), f)
                            hue = f[0]; sat = f[1]; value = f[2]
                            emit(q)
                        },
                )
            }
        }
    }
}
