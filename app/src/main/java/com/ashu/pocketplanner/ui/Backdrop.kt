package com.ashu.pocketplanner.ui

import android.provider.Settings as SysSettings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.ashu.pocketplanner.data.Settings
import com.ashu.pocketplanner.data.ThemePreset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun rememberMotionOn(): Boolean {
    val ctx = LocalContext.current
    return remember {
        runCatching { SysSettings.Global.getFloat(ctx.contentResolver, SysSettings.Global.ANIMATOR_DURATION_SCALE, 1f) }
            .getOrDefault(1f) > 0f
    }
}

/** The colorful moving fog behind everything, or your own photo. */
@Composable
fun AppBackground(settings: Settings, photo: ImageBitmap?) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().background(p.base)) {
        if (settings.theme == ThemePreset.PHOTO && photo != null) {
            Image(photo, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (!p.dark) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.28f)))
        } else {
            Blobs(p)
            if (p.dots) DotGrid(p)
        }
    }
}

@Composable
private fun Blobs(p: Palette) {
    val motion = rememberMotionOn()
    val t = rememberInfiniteTransition(label = "blobs")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val a0 = if (motion) phase else 0.8f
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        p.blobs.forEachIndexed { i, col ->
            val a = a0 + i * 1.7f
            val cx = w * (0.5f + 0.38f * cos(a * (1f + i * 0.13f)))
            val cy = h * (0.42f + 0.36f * sin(a * 0.8f + i))
            val r = max(w, h) * (0.42f + 0.06f * (i % 3))
            drawCircle(
                brush = Brush.radialGradient(listOf(col.copy(alpha = p.blobAlpha), Color.Transparent), center = Offset(cx, cy), radius = r),
                radius = r,
                center = Offset(cx, cy),
            )
        }
    }
}

@Composable
private fun DotGrid(p: Palette) {
    Canvas(Modifier.fillMaxSize()) {
        val step = 18f
        val dot = if (p.dark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.06f)
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(dot, radius = 1.6f, center = Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

data class Shot(val id: Int, val big: Boolean)

private class Piece(val x0: Float, val vx: Float, val vy: Float, val color: Color, val spin: Float, val w: Float, val h: Float)

/** Confetti burst from the middle of the screen. */
@Composable
fun ConfettiOverlay(shot: Shot) {
    if (shot.id == 0) return
    val p = LocalPalette.current
    val anim = remember(shot.id) { Animatable(0f) }
    val pieces = remember(shot.id) {
        val colors = listOf(p.accent, p.warm, Color(0xFFFF5C9A), Color(0xFF5EF2C9), Color(0xFF7FB2FF), Color.White)
        val n = if (shot.big) 150 else 60
        List(n) {
            Piece(
                x0 = 0.5f + (Random.nextFloat() - 0.5f) * 0.2f,
                vx = (Random.nextFloat() - 0.5f) * (if (shot.big) 1.3f else 0.8f),
                vy = -(0.35f + Random.nextFloat() * (if (shot.big) 0.75f else 0.45f)),
                color = colors[it % colors.size],
                spin = (Random.nextFloat() - 0.5f) * 4f,
                w = 8f + Random.nextFloat() * 10f,
                h = 5f + Random.nextFloat() * 5f,
            )
        }
    }
    LaunchedEffect(shot.id) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(if (shot.big) 2200 else 1400, easing = LinearEasing))
    }
    Canvas(Modifier.fillMaxSize()) {
        val t = anim.value
        if (t >= 1f) return@Canvas
        pieces.forEach { pc ->
            val x = size.width * (pc.x0 + pc.vx * t)
            val y = size.height * (0.42f + pc.vy * t + 1.25f * t * t)
            rotate(degrees = pc.spin * 360f * t, pivot = Offset(x, y)) {
                drawRect(
                    color = pc.color.copy(alpha = (1f - t * t).coerceIn(0f, 1f)),
                    topLeft = Offset(x - pc.w / 2, y - pc.h / 2),
                    size = Size(pc.w, pc.h),
                )
            }
        }
    }
}
