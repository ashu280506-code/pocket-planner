package com.ashu.pocketplanner.fx

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphManager

/**
 * Lights on the back of Nothing phones. Only works while the app is open (Nothing's rule),
 * and silently does nothing on other phones.
 */
object GlyphFx {
    private var gm: GlyphManager? = null
    @Volatile private var ready = false
    private val handler = Handler(Looper.getMainLooper())
    private val offRunnable = Runnable { runCatching { gm?.turnOff() } }

    fun supported(): Boolean =
        Build.VERSION.SDK_INT >= 34 && Build.MANUFACTURER.equals("Nothing", ignoreCase = true)

    fun start(ctx: Context) {
        if (!supported() || gm != null) return
        try {
            val m = GlyphManager.getInstance(ctx.applicationContext)
            gm = m
            m.init(object : GlyphManager.Callback {
                override fun onServiceConnected(componentName: ComponentName) {
                    try {
                        register(m)
                        m.openSession()
                        ready = true
                    } catch (_: Throwable) {
                        ready = false
                    }
                }

                override fun onServiceDisconnected(componentName: ComponentName) {
                    ready = false
                }
            })
        } catch (_: Throwable) {
            gm = null
        }
    }

    private fun register(m: GlyphManager) {
        when {
            Common.is24111() -> m.register(Glyph.DEVICE_24111)
            Common.is23111() -> m.register(Glyph.DEVICE_23111)
            Common.is23113() -> m.register(Glyph.DEVICE_23113)
            Common.is22111() -> m.register(Glyph.DEVICE_22111)
            Common.is20111() -> m.register(Glyph.DEVICE_20111)
            Common.is25111() -> m.register(Glyph.DEVICE_25111)
            Common.is25131() -> m.register(Glyph.DEVICE_25131)
            else -> m.register()
        }
    }

    fun stop() {
        handler.removeCallbacks(offRunnable)
        val m = gm ?: return
        runCatching { m.turnOff() }
        runCatching { if (ready) m.closeSession() }
        runCatching { m.unInit() }
        ready = false
        gm = null
    }

    private fun offIn(ms: Long) {
        handler.removeCallbacks(offRunnable)
        handler.postDelayed(offRunnable, ms)
    }

    /** Fills the long light strip like a progress bar for today. */
    fun progress(percent: Int) {
        val m = gm ?: return
        if (!ready) return
        runCatching {
            val frame = m.glyphFrameBuilder.buildChannelC().build()
            m.displayProgress(frame, percent.coerceIn(0, 100))
            offIn(2600)
        }
    }

    /** Quick blink when you tick something off. */
    fun flash() {
        val m = gm ?: return
        if (!ready) return
        runCatching {
            val frame = m.glyphFrameBuilder.buildChannelA().buildChannelB()
                .buildPeriod(450).buildCycles(1).buildInterval(0).build()
            m.animate(frame)
        }
    }

    /** Everything breathes a few times when the whole day is done. */
    fun celebrate() {
        val m = gm ?: return
        if (!ready) return
        runCatching {
            val frame = m.glyphFrameBuilder.buildChannelA().buildChannelB().buildChannelC()
                .buildPeriod(900).buildCycles(3).buildInterval(120).build()
            m.animate(frame)
            offIn(3600)
        }
    }
}
