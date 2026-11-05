package com.ashu.pocketplanner.fx

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Your background photos. The original is kept so the fog level can be changed later;
 * the foggy version is made by shrinking and re-enlarging the picture (a soft blur) and dimming it.
 */
object Photos {
    private fun file(ctx: Context, name: String) = File(ctx.filesDir, name)

    fun importApp(ctx: Context, uri: Uri, fog: Float): Boolean {
        return try {
            val b = decode(ctx, uri, 1440) ?: return false
            save(b, file(ctx, "app_src.jpg"), 92)
            renderApp(ctx, fog)
        } catch (_: Throwable) {
            false
        }
    }

    fun renderApp(ctx: Context, fog: Float): Boolean {
        return try {
            val src = BitmapFactory.decodeFile(file(ctx, "app_src.jpg").path) ?: return false
            save(fogify(src, fog, 1080, 0.10f + fog * 0.22f), file(ctx, "app.jpg"), 90)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun loadApp(ctx: Context): Bitmap? = runCatching { BitmapFactory.decodeFile(file(ctx, "app.jpg").path) }.getOrNull()

    fun removeApp(ctx: Context) {
        file(ctx, "app_src.jpg").delete()
        file(ctx, "app.jpg").delete()
    }

    fun importWidget(ctx: Context, uri: Uri, fog: Float): Boolean {
        return try {
            val b = decode(ctx, uri, 1080) ?: return false
            save(b, file(ctx, "widget_src.jpg"), 92)
            renderWidget(ctx, fog)
        } catch (_: Throwable) {
            false
        }
    }

    /** Kept small on purpose: widgets have a tight size limit for pictures. */
    fun renderWidget(ctx: Context, fog: Float): Boolean {
        return try {
            val src = BitmapFactory.decodeFile(file(ctx, "widget_src.jpg").path) ?: return false
            save(fogify(src, fog, 420, 0.18f + fog * 0.24f), file(ctx, "widget.jpg"), 88)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun loadWidget(ctx: Context): Bitmap? = runCatching {
        BitmapFactory.decodeFile(
            file(ctx, "widget.jpg").path,
            BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
        )
    }.getOrNull()

    fun removeWidget(ctx: Context) {
        file(ctx, "widget_src.jpg").delete()
        file(ctx, "widget.jpg").delete()
    }

    private fun save(b: Bitmap, f: File, quality: Int) {
        f.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, quality, it) }
    }

    private fun decode(ctx: Context, uri: Uri, maxSide: Int): Bitmap? {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val raw = cr.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = runCatching {
            cr.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        val scale = minOf(1f, maxSide.toFloat() / max(raw.width, raw.height))
        val m = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true)
    }

    private fun fogify(src: Bitmap, fog: Float, outW: Int, dim: Float): Bitmap {
        val f = fog.coerceIn(0f, 1f)
        val w = minOf(outW, src.width).coerceAtLeast(16)
        val h = (src.height * w / src.width.toFloat()).roundToInt().coerceAtLeast(16)
        val smallW = (w * (1f - 0.95f * f)).roundToInt().coerceAtLeast(10)
        val smallH = (smallW * h / w.toFloat()).roundToInt().coerceAtLeast(10)
        val small = Bitmap.createScaledBitmap(src, smallW, smallH, true)
        val mid = Bitmap.createScaledBitmap(small, minOf(w, smallW * 3), minOf(h, smallH * 3), true)
        val out = Bitmap.createScaledBitmap(mid, w, h, true).copy(Bitmap.Config.ARGB_8888, true)
        Canvas(out).drawColor(Color.argb((dim.coerceIn(0f, 0.8f) * 255).roundToInt(), 10, 8, 24))
        return out
    }
}
